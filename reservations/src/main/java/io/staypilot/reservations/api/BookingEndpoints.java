package io.staypilot.reservations.api;

import io.staypilot.reservations.api.ReservationDtos.*;
import io.staypilot.reservations.application.BookingCoordinator;
import io.staypilot.reservations.model.BookingStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingEndpoints {

    private final BookingCoordinator bookings;

    public BookingEndpoints(BookingCoordinator bookings) {
        this.bookings = bookings;
    }

    @PostMapping
    public ResponseEntity<BookingView> reserve(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NewBooking request) {
        BookingView result = bookings.reserve(jwt.getSubject(), request);
        return ResponseEntity.status(result.status() == BookingStatus.PENDING ? HttpStatus.ACCEPTED :
            result.status() == BookingStatus.CONFIRMED ? HttpStatus.CREATED : HttpStatus.OK).body(result);
    }

    @GetMapping
    public Page<BookingView> history(@AuthenticationPrincipal Jwt jwt, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable page) {
        return bookings.history(jwt.getSubject(), page);
    }

    @GetMapping("/{id}")
    public BookingView one(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return bookings.get(jwt.getSubject(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BookingView> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        BookingView result = bookings.cancel(jwt.getSubject(), id);
        return ResponseEntity.status(result.compensationPending() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(result);
    }
}
