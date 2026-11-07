package io.staypilot.reservations.application;

import io.staypilot.contracts.TokenSupport;
import io.staypilot.reservations.api.ReservationDtos.TokenResponse;
import io.staypilot.reservations.model.Account;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JwtIssuer {

    private final JwtEncoder encoder;

    public JwtIssuer(@Value("${security.jwt.secret}") String secret) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(TokenSupport.secretKey(secret)));
    }

    public TokenResponse issue(Account user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("staypilot")
            .subject(user.getUsername()).issuedAt(now).expiresAt(now.plusSeconds(3600))
            .claim("role", user.getRole().name()).build();
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return new TokenResponse(encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue(), "Bearer", 3600);
    }
}
