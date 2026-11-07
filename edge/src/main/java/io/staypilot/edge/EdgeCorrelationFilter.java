package io.staypilot.edge;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class EdgeCorrelationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String candidate = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        String id = candidate != null && candidate.matches("[a-zA-Z0-9-]{1,64}") ? candidate : UUID.randomUUID().toString();
        exchange.getResponse().getHeaders().set("X-Correlation-Id", id);
        return chain.filter(exchange.mutate().request(exchange.getRequest().mutate().headers(headers -> {
            headers.remove("X-Internal-Key");
            headers.set("X-Correlation-Id", id);
        }).build()).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
