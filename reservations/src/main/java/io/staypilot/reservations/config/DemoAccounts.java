
package io.staypilot.reservations.config;

import io.staypilot.reservations.model.*;
import io.staypilot.reservations.persistence.Accounts;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoAccounts {

    @Bean
    ApplicationRunner seedDemoUsers(
        Accounts accounts,
        PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (accounts.existsByUsername("operator")) {
                return;
            }

            accounts.save(new Account(
                "operator",
                passwordEncoder.encode("Operator123!"),
                Role.ADMIN
            ));

            accounts.save(new Account(
                "traveler",
                passwordEncoder.encode("Traveler123!"),
                Role.USER
            ));
        };
    }
}
