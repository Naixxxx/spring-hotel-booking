package io.staypilot.reservations.api;

import io.staypilot.reservations.api.ReservationDtos.*;
import io.staypilot.reservations.application.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountsEndpoints {

    private final AccountService accounts;

    public AdminAccountsEndpoints(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<UserView> all() {
        return accounts.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody AdminUserWrite request) {
        return accounts.adminCreate(request);
    }

    @PatchMapping("/{id}")
    public UserView change(@PathVariable Long id, @RequestBody AdminUserPatch request) {
        return accounts.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        accounts.delete(id, jwt.getSubject());
    }
}
