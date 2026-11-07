package io.staypilot.reservations.persistence;

import io.staypilot.reservations.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Bookings extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByRequestId(UUID requestId);

    boolean existsByAccountId(Long accountId);

    Page<Reservation> findByAccountId(Long userId, Pageable page);

    List<Reservation> findByStatusAndCompensationPendingTrue(BookingStatus status);
}
