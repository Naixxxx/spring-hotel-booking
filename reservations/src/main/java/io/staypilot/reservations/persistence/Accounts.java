package io.staypilot.reservations.persistence;

import io.staypilot.reservations.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Accounts extends JpaRepository<Account, Long> {

    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);
}
