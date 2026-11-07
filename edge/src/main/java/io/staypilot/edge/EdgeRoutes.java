package io.staypilot.edge;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EdgeRoutes {

    @Bean
    RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("reservations", route -> route.path("/api/bookings", "/api/bookings/**",
                "/api/auth/**", "/api/admin/users", "/api/admin/users/**").uri("lb://reservation-service"))
            .route("catalog", route -> route.path("/api/hotels", "/api/hotels/**",
                "/api/rooms", "/api/rooms/**").uri("lb://catalog-service"))
            .build();
    }
}
