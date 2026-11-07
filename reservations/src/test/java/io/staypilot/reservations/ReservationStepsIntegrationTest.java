package io.staypilot.reservations;

import io.staypilot.contracts.DomainError;
import io.staypilot.reservations.application.ReservationSteps;
import io.staypilot.reservations.api.ReservationDtos.NewBooking;
import io.staypilot.reservations.persistence.Accounts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false", "staypilot.compensation.delay-ms=9999999"})
class ReservationStepsIntegrationTest {

    @Autowired
    ReservationSteps steps;
    @Autowired
    Accounts accounts;

    @Test
    void requestIdIsUniqueAndCannotBeReusedWithDifferentInput() {
        Long userId = accounts.findByUsername("traveler").orElseThrow().getId();
        UUID requestId = UUID.randomUUID();
        LocalDate start = LocalDate.now().plusDays(21);
        NewBooking request = new NewBooking(requestId, 3L, false, start, start.plusDays(2));
        ReservationSteps.Begin first = steps.start(userId, request, "same-parameters");
        ReservationSteps.Begin replay = steps.start(userId, request, "same-parameters");
        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(first.bookingId()).isEqualTo(replay.bookingId());
        assertThatThrownBy(() -> steps.start(userId, request, "changed-parameters"))
            .isInstanceOf(DomainError.class).hasMessageContaining("requestId");
    }
}
