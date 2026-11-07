package io.staypilot.edge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class EdgeSecurityTest {

    @Autowired
    WebTestClient client;

    @Test
    void anonymousUsersCannotBook() {
        client.post().uri("/api/bookings").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void internalResourceNotExposed() {
        client.post().uri("/internal/rooms/1/hold").exchange().expectStatus().is4xxClientError();
    }
}
