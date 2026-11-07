package io.staypilot.reservations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false", "staypilot.compensation.delay-ms=9999999"})
@AutoConfigureMockMvc
class ReservationSecurityMvcTest {

    @Autowired
    MockMvc mock;

    @Test
    void anonymousCannotViewBookings() throws Exception {
        mock.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
    }

    @Test
    void normalUserCannotListAccounts() throws Exception {
        mock.perform(get("/api/admin/users").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void newAccountCanRegister() throws Exception {
        mock.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"username\":\"api-person\",\"password\":\"Abcd12345!\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void invalidBookingDatesAreRejected() throws Exception {
        mock.perform(post("/api/bookings").with(jwt().jwt(jwt -> jwt.subject("traveler"))
                    .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType("application/json").content("{\"requestId\":\"" + java.util.UUID.randomUUID() + "\",\"autoSelect\":true,\"startDate\":\"2000-01-01\",\"endDate\":\"2000-01-02\"}"))
            .andExpect(status().isBadRequest());
    }
}
