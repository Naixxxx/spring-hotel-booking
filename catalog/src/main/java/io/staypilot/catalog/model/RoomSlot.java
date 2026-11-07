package io.staypilot.catalog.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "room_slots", indexes = {
    @Index(name = "idx_room_slots_overlap", columnList = "room_id,state,start_date,end_date")})
public class RoomSlot {

    @Id
    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomLock state;
    @Column(nullable = false)
    private boolean counted;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Version
    private Long version;

    protected RoomSlot() {
    }

    public RoomSlot(UUID bookingId, Room room, LocalDate startDate, LocalDate endDate, RoomLock state, boolean counted) {
        this.bookingId = bookingId;
        this.room = room;
        this.startDate = startDate;
        this.endDate = endDate;
        this.state = state;
        this.counted = counted;
        this.createdAt = Instant.now();
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public RoomLock getState() {
        return state;
    }

    public boolean isCounted() {
        return counted;
    }

    public void release() {
        state = RoomLock.RELEASED;
    }

    public void uncount() {
        counted = false;
    }
}
