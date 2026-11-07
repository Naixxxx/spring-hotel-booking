package io.staypilot.catalog.application;

import io.staypilot.catalog.model.*;
import io.staypilot.catalog.persistence.*;
import io.staypilot.contracts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SlotLedger {

    private static final Logger log = LoggerFactory.getLogger(SlotLedger.class);
    private final Rooms rooms;
    private final RoomSlots slots;

    public SlotLedger(Rooms rooms, RoomSlots slots) {
        this.rooms = rooms;
        this.slots = slots;
    }

    @Transactional
    public HoldReceipt hold(Long roomId, HoldCommand cmd) {
        if (cmd.bookingId() == null)
            throw new DomainError(HttpStatus.BAD_REQUEST, "MISSING_BOOKING_ID", "bookingId is required");
        RoomDirectory.validateRange(cmd.startDate(), cmd.endDate());
        Room room = lock(roomId); // Serializes all overlap checks for the same room.
        try (MDC.MDCCloseable ignored = MDC.putCloseable("bookingId", cmd.bookingId().toString())) {
            RoomSlot known = slots.findById(cmd.bookingId()).orElse(null);
            if (known != null) {
                if (!known.getRoom().getId().equals(roomId) || !cmd.startDate().equals(known.getStartDate())
                    || !cmd.endDate().equals(known.getEndDate()) || known.getState() != RoomLock.HELD)
                    throw new DomainError(HttpStatus.CONFLICT, "REPLAY_CONFLICT", "Reservation already processed or released");
                log.info("event=hold_replayed roomId={}", roomId);
                return new HoldReceipt(cmd.bookingId(), roomId, "HELD");
            }
            if (!room.isAvailable() || slots.overlaps(roomId, cmd.startDate(), cmd.endDate()))
                throw new DomainError(HttpStatus.CONFLICT, "ROOM_OCCUPIED", "Room unavailable for the requested dates");
            slots.save(new RoomSlot(cmd.bookingId(), room, cmd.startDate(), cmd.endDate(), RoomLock.HELD, true));
            room.acquired();
            log.info("event=hold_created roomId={} start={} end={}", roomId, cmd.startDate(), cmd.endDate());
            return new HoldReceipt(cmd.bookingId(), roomId, "HELD");
        }
    }

    @Transactional
    public HoldReceipt release(Long roomId, ReleaseCommand cmd) {
        if (cmd.bookingId() == null)
            throw new DomainError(HttpStatus.BAD_REQUEST, "MISSING_BOOKING_ID", "bookingId is required");
        Room room = lock(roomId);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("bookingId", cmd.bookingId().toString())) {
            RoomSlot existing = slots.findById(cmd.bookingId()).orElse(null);
            if (existing == null) {
                // Tombstone protects against an in-flight delayed hold arriving after compensation.
                slots.save(new RoomSlot(cmd.bookingId(), room, null, null, RoomLock.RELEASED, false));
                log.info("event=release_tombstone roomId={}", roomId);
            } else {
                if (!existing.getRoom().getId().equals(roomId))
                    throw new DomainError(HttpStatus.CONFLICT, "REPLAY_CONFLICT", "Booking belongs to another room");
                if (existing.getState() == RoomLock.HELD) {
                    existing.release();
                    if (cmd.compensation() && existing.isCounted()) {
                        room.compensated();
                        existing.uncount();
                    }
                    log.info("event=slot_released roomId={} compensation={}", roomId, cmd.compensation());
                } else {
                    log.info("event=release_replayed roomId={}", roomId);
                }
            }
            return new HoldReceipt(cmd.bookingId(), roomId, "RELEASED");
        }
    }

    private Room lock(Long roomId) {
        return rooms.lockById(roomId).orElseThrow(() -> new DomainError(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Room not found: " + roomId));
    }
}
