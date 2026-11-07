package io.staypilot.reservations.api;

import io.staypilot.reservations.api.ReservationDtos.*;
import io.staypilot.reservations.application.*;
import io.staypilot.reservations.model.Role;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class IdentityEndpoints {

    private final AccountService accounts;
    private final JwtIssuer issuer;

    public IdentityEndpoints(AccountService accounts, JwtIssuer issuer) {
        this.accounts = accounts;
        this.issuer = issuer;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody NewAccount input) {
        return issuer.issue(accounts.register(input.username(), input.password(), Role.USER));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody Credentials input) {
        return issuer.issue(accounts.authenticate(input.username(), input.password()));
    }
}
