package io.staypilot.reservations.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.staypilot.contracts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

@Component
public class HttpCatalogAdapter implements CatalogPort {

    private static final Logger log = LoggerFactory.getLogger(HttpCatalogAdapter.class);
    private final RestClient http;
    private final String key;
    private final RetryTemplate retry;
    private final CircuitBreaker breaker;

    public HttpCatalogAdapter(RestClient.Builder builder, @Value("${security.internal.key}") String key,
                              RetryTemplate catalogRetry, CircuitBreaker catalogCircuitBreaker) {
        this.http = builder.baseUrl("http://catalog-service").build();
        this.key = key;
        this.retry = catalogRetry;
        this.breaker = catalogCircuitBreaker;
    }

    @Override
    public List<RoomCard> rankedRooms(LocalDate from, LocalDate to) {
        RoomCard[] cards = remote(() -> http.get()
            .uri(uriBuilder -> uriBuilder
                .path("/internal/rooms/recommend")
                .queryParam("startDate", from)
                .queryParam("endDate", to)
                .build())
            .header("X-Internal-Key", key)
            .retrieve()
            .body(RoomCard[].class));

        return cards == null ? List.of() : Arrays.asList(cards);
    }

    @Override
    public void hold(Long roomId, UUID bookingId, LocalDate from, LocalDate to) {
        remote(() -> {
            http.post().uri("/internal/rooms/{id}/hold", roomId)
                .header("X-Internal-Key", key).header("X-Correlation-Id", bookingId.toString())
                .body(new HoldCommand(bookingId, from, to)).retrieve().toBodilessEntity();
            return null;
        });
    }

    @Override
    public void release(Long roomId, UUID bookingId, boolean compensation) {
        remote(() -> {
            http.post().uri("/internal/rooms/{id}/release", roomId)
                .header("X-Internal-Key", key).header("X-Correlation-Id", bookingId.toString())
                .body(new ReleaseCommand(bookingId, compensation)).retrieve().toBodilessEntity();
            return null;
        });
    }

    private <T> T remote(Callable<T> action) {
        return retry.execute(context -> {
            if (context.getRetryCount() > 0) log.warn("event=catalog_retry attempt={}", context.getRetryCount() + 1);
            try {
                return breaker.executeCallable(action);
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 404)
                    throw new SlotBusyException("Catalog rejected the room request: " + e.getStatusCode().value());
                throw new RemoteHotelTransportException("Catalog HTTP " + e.getStatusCode().value(), e);
            } catch (ResourceAccessException e) {
                throw new RemoteHotelTransportException("Catalog timeout or connection failure", e);
            } catch (Exception e) {
                throw new RemoteHotelTransportException("Unexpected catalog client error", e);
            }
        });
    }
}
