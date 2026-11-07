package io.staypilot.reservations.application;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.staypilot.contracts.*;
import io.staypilot.reservations.api.ReservationDtos.*;
import io.staypilot.reservations.integration.*;
import io.staypilot.reservations.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class BookingCoordinator {

    private static final Logger log = LoggerFactory.getLogger(BookingCoordinator.class);
    private final ReservationSteps steps;
    private final AccountService accounts;
    private final CatalogPort catalog;

    public BookingCoordinator(ReservationSteps steps, AccountService accounts, CatalogPort catalog) {
        this.steps = steps;
        this.accounts = accounts;
        this.catalog = catalog;
    }

    public BookingView reserve(String actor, NewBooking request) {
        validate(request);
        Account user = accounts.get(actor);
        String fingerprint = user.getId() + "|" + request.autoSelect() + "|" + (request.autoSelect() ? null : request.roomId()) + "|" + request.startDate() + "|" + request.endDate();
        ReservationSteps.Begin started;
        try {
            started = steps.start(user.getId(), request, fingerprint);
        } catch (DataIntegrityViolationException concurrentReplay) {
            // The unique index is the final arbiter when two threads use one requestId.
            BookingView existing = stepsByRequest(request.requestId(), user.getId(), fingerprint);
            return existing;
        }
        UUID id = started.bookingId();
        if (!started.created()) return steps.get(id);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("bookingId", id.toString())) {
            log.info("event=booking_pending requestId={} userId={}", request.requestId(), user.getId());
            List<Long> options;
            try {
                options = request.autoSelect()
                    ? catalog.rankedRooms(request.startDate(), request.endDate()).stream().map(RoomCard::id).toList()
                    : List.of(request.roomId());
            } catch (RuntimeException e) {
                abort(id, request.roomId(), "CATALOG_UNAVAILABLE", e);
                throw unavailable();
            }
            for (Long roomId : options) {
                steps.select(id, roomId);
                try {
                    catalog.hold(roomId, id, request.startDate(), request.endDate());
                } catch (SlotBusyException conflict) {
                    log.info("event=room_conflict roomId={}", roomId);
                    if (request.autoSelect()) continue;
                    abort(id, null, "ROOM_OCCUPIED", conflict);
                    throw new DomainError(HttpStatus.CONFLICT, "ROOM_OCCUPIED", conflict.getMessage());
                } catch (RuntimeException transport) {
                    abort(id, roomId, "HOLD_UNCERTAIN", transport);
                    throw unavailable();
                }
                try {
                    steps.confirm(id);
                    log.info("event=booking_confirmed roomId={}", roomId);
                    return steps.get(id);
                } catch (RuntimeException dbFailure) {
                    abort(id, roomId, "CONFIRM_FAILED", dbFailure);
                    throw unavailable();
                }
            }
            abort(id, null, "NO_AVAILABLE_ROOMS", null);
            throw new DomainError(HttpStatus.CONFLICT, "NO_AVAILABLE_ROOMS", "No rooms are available for these dates");
        }
    }

    /**
     * In a full deployment, the request lookup is backed by the same unique index as start().
     */
    private BookingView stepsByRequest(UUID requestId, Long userId, String fingerprint) {
        return steps.getByRequest(requestId, userId, fingerprint);
    }

    private void abort(UUID id, Long uncertainRoom, String reason, Throwable error) {
        if (error != null)
            log.warn("event=booking_compensation_started reason={} message={}", reason, error.toString());
        steps.cancelled(id, reason, uncertainRoom != null);
        if (uncertainRoom != null) completeRelease(id, uncertainRoom, true);
        log.info("event=booking_cancelled reason={}", reason);
    }

    private void completeRelease(UUID id, Long roomId, boolean compensation) {
        try {
            catalog.release(roomId, id, compensation);
            steps.released(id);
            log.info("event=compensation_completed roomId={} compensation={}", roomId, compensation);
        } catch (RuntimeException e) {
            log.error("event=compensation_queued roomId={} cause={}", roomId, e.toString());
            // Durable compensationPending=true is retried by the scheduled recovery job.
        }
    }

    private DomainError unavailable() {
        return new DomainError(HttpStatus.SERVICE_UNAVAILABLE, "HOTEL_SERVICE_UNAVAILABLE", "Room verification failed; booking was cancelled");
    }

    public BookingView get(String actor, UUID id) {
        BookingView view = steps.get(id);
        assertOwner(actor, view);
        return view;
    }

    public Page<BookingView> history(String actor, Pageable page) {
        return steps.history(accounts.get(actor).getId(), page);
    }

    public BookingView cancel(String actor, UUID id) {
        BookingView current = get(actor, id);
        if (current.status() == BookingStatus.PENDING)
            throw new DomainError(HttpStatus.CONFLICT, "BOOKING_IN_PROGRESS", "Booking is still being confirmed");
        if (current.status() == BookingStatus.CANCELLED) {
            if (current.compensationPending() && current.roomId() != null)
                completeRelease(id, current.roomId(), !"USER_CANCELLED".equals(current.failureReason()));
            return steps.get(id);
        }
        steps.cancelled(id, "USER_CANCELLED", true);
        completeRelease(id, current.roomId(), false);
        return steps.get(id);
    }

    @Scheduled(fixedDelayString = "${staypilot.compensation.delay-ms:15000}")
    public void retryPendingReleases() {
        for (BookingView job : steps.pendingReleases()) {
            if (job.roomId() == null) {
                steps.released(job.id());
                continue;
            }
            try (MDC.MDCCloseable ignored = MDC.putCloseable("bookingId", job.id().toString())) {
                completeRelease(job.id(), job.roomId(), !"USER_CANCELLED".equals(job.failureReason()));
            }
        }
    }

    private void assertOwner(String actor, BookingView booking) {
        Account user = accounts.get(actor);
        if (user.getRole() != Role.ADMIN && !user.getId().equals(booking.userId()))
            throw new DomainError(HttpStatus.FORBIDDEN, "NOT_YOUR_BOOKING", "Access to another user's booking is forbidden");
    }

    private void validate(NewBooking request) {
        if (request.requestId() == null)
            throw new DomainError(HttpStatus.BAD_REQUEST, "MISSING_REQUEST_ID", "requestId required");
        LocalDate start = request.startDate(), end = request.endDate();
        if (start == null || end == null || start.isBefore(LocalDate.now()) || !start.isBefore(end))
            throw new DomainError(HttpStatus.BAD_REQUEST, "INVALID_DATES", "Use startDate >= today and endDate > startDate");
        if (!request.autoSelect() && request.roomId() == null)
            throw new DomainError(HttpStatus.BAD_REQUEST, "ROOM_REQUIRED", "Supply roomId when autoSelect is false");
    }
}
