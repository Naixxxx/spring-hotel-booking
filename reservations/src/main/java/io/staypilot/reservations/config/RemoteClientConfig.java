package io.staypilot.reservations.config;

import io.staypilot.reservations.integration.RemoteHotelTransportException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Configuration
public class RemoteClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder inventoryClientBuilder() {
        var jdk = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(800)).build());
        jdk.setReadTimeout(Duration.ofMillis(1500));
        return RestClient.builder().requestFactory(jdk);
    }

    @Bean
    RetryTemplate catalogRetry() {
        RetryTemplate retry = new RetryTemplate();
        retry.setRetryPolicy(new SimpleRetryPolicy(3, Map.of(RemoteHotelTransportException.class, true)));
        ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
        backoff.setInitialInterval(150);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(800);
        retry.setBackOffPolicy(backoff);
        return retry;
    }

    @Bean
    CircuitBreaker catalogCircuitBreaker() {
        return CircuitBreaker.ofDefaults("catalog-http");
    }
}
