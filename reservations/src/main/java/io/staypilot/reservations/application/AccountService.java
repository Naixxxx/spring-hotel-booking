package io.staypilot.reservations.application;

import io.staypilot.contracts.DomainError;
import io.staypilot.reservations.api.ReservationDtos.*;
import io.staypilot.reservations.model.*;
import io.staypilot.reservations.persistence.Accounts;
import io.staypilot.reservations.persistence.Bookings;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final Accounts accounts;
    private final Bookings bookings;
    private final PasswordEncoder passwordEncoder;

    public AccountService(Accounts accounts, Bookings bookings, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.bookings = bookings;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Account register(String username, String password, Role role) {
        if (accounts.existsByUsername(username))
            throw new DomainError(HttpStatus.CONFLICT, "LOGIN_TAKEN", "Username already registered");
        return accounts.save(new Account(username, passwordEncoder.encode(password), role));
    }

    @Transactional(readOnly = true)
    public Account get(String username) {
        return accounts.findByUsername(username).orElseThrow(() -> new DomainError(HttpStatus.UNAUTHORIZED, "UNKNOWN_USER", "Account not found"));
    }

    @Transactional(readOnly = true)
    public Account authenticate(String username, String password) {
        Account user = accounts.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash()))
            throw new DomainError(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid username or password");
        return user;
    }

    @Transactional(readOnly = true)
    public List<UserView> list() {
        return accounts.findAll().stream().map(this::view).toList();
    }

    @Transactional
    public UserView adminCreate(AdminUserWrite request) {
        return view(register(request.username(), request.password(), roleOf(request.role())));
    }

    @Transactional
    public UserView update(Long id, AdminUserPatch request) {
        Account user = byId(id);
        if (request.password() != null && request.password().length() < 8)
            throw new DomainError(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Password must have at least 8 characters");
        user.update(request.password() == null ? null : passwordEncoder.encode(request.password()),
            request.role() == null ? null : roleOf(request.role()));
        return view(user);
    }

    @Transactional
    public void delete(Long id, String actor) {
        Account user = byId(id);
        if (user.getUsername().equals(actor))
            throw new DomainError(HttpStatus.CONFLICT, "SELF_DELETION", "Cannot delete your own account");
        // FK from bookings prevents silent loss of financial/operational history.
        if (bookings.existsByAccountId(id))
            throw new DomainError(HttpStatus.CONFLICT, "ACCOUNT_HAS_BOOKINGS", "Cannot delete a user with booking history");
        accounts.delete(user);
    }

    private Account byId(Long id) {
        return accounts.findById(id).orElseThrow(() -> new DomainError(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Account not found"));
    }

    private Role roleOf(String name) {
        try {
            return Role.valueOf(name);
        } catch (Exception e) {
            throw new DomainError(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Use USER or ADMIN");
        }
    }

    private UserView view(Account account) {
        return new UserView(account.getId(), account.getUsername(), account.getRole().name());
    }
}
