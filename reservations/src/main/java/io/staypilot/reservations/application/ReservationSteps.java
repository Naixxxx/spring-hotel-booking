package io.staypilot.reservations.application;

import io.staypilot.contracts.DomainError;
import io.staypilot.reservations.api.ReservationDtos.*;
import io.staypilot.reservations.model.*;
import io.staypilot.reservations.persistence.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Only database state transitions live here. No outgoing HTTP inside @Transactional.
 */
@Service
public class ReservationSteps {

    private final Bookings bookings;
    private final Accounts accounts;

    public ReservationSteps(Bookings bookings, Accounts accounts) {
        this.bookings = bookings;
        this.accounts = accounts;
    }

    public record Begin(UUID bookingId, boolean created) {
    }

    @Transactional
    public Begin start(Long userId, NewBooking request, String fingerprint) {
        Reservation existing = bookings.findByRequestId(request.requestId()).orElse(null);
        if (existing != null) {
            if (!existing.getFingerprint().equals(fingerprint) || !existing.getAccount().getId().equals(userId))
                throw new DomainError(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "requestId was used with different parameters");
            return new Begin(existing.getId(), false);
        }
        Account account = accounts.getReferenceById(userId);
        Reservation reservation = bookings.saveAndFlush(new Reservation(request.requestId(), fingerprint, account,
            request.autoSelect() ? null : request.roomId(), request.startDate(), request.endDate(), request.autoSelect()));
        return new Begin(reservation.getId(), true);
    }

    @Transactional(readOnly = true)
    public BookingView getByRequest(UUID requestId, Long userId, String fingerprint) {
        Reservation found = bookings.findByRequestId(requestId).orElseThrow(() -> new DomainError(
            HttpStatus.CONFLICT, "REQUEST_STILL_PROCESSING", "Concurrent request not yet committed"));
        if (!found.getAccount().getId().equals(userId) || !found.getFingerprint().equals(fingerprint))
            throw new DomainError(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "requestId already used");
        return view(found);
    }

    @Transactional
    public void select(UUID id, Long roomId) {
        needed(id).select(roomId);
    }

    @Transactional
    public void confirm(UUID id) {
        needed(id).confirm();
    }

    @Transactional
    public void cancelled(UUID id, String why, boolean pending) {
        needed(id).cancel(why, pending);
    }

    @Transactional
    public void released(UUID id) {
        needed(id).releaseCompleted();
    }

    @Transactional(readOnly = true)
    public BookingView get(UUID id) {
        return view(needed(id));
    }

    @Transactional(readOnly = true)
    public Page<BookingView> history(Long accountId, Pageable page) {
        return bookings.findByAccountId(accountId, page).map(this::view);
    }

    @Transactional(readOnly = true)
    public java.util.List<BookingView> pendingReleases() {
        return bookings.findByStatusAndCompensationPendingTrue(BookingStatus.CANCELLED).stream().map(this::view).toList();
    }

    private Reservation needed(UUID id) {
        return bookings.findById(id).orElseThrow(() -> new DomainError(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking not found"));
    }

    private BookingView view(Reservation booking) {
        return new BookingView(booking.getId(), booking.getRequestId(), booking.getAccount().getId(), booking.getRoomId(),
            booking.getStartDate(), booking.getEndDate(), booking.getStatus(), booking.isCompensationPending(),
            booking.getFailureReason(), booking.getCreatedAt());
    }
}
