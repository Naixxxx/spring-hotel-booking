package io.staypilot.catalog.config;

import io.staypilot.contracts.TokenSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
@EnableMethodSecurity
public class CatalogSecurity {

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        return TokenSupport.decoder(secret);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, @Value("${security.internal.key}") String key) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/hotels/**", "/api/rooms/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/hotels/**", "/api/rooms/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/hotels/**", "/api/rooms/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(TokenSupport.authorities())))
            .addFilterBefore(new InternalKeyFilter(key), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    static final class InternalKeyFilter extends OncePerRequestFilter {

        private final byte[] expected;

        InternalKeyFilter(String key) {
            expected = key.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !request.getRequestURI().startsWith("/internal/");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            String presented = request.getHeader("X-Internal-Key");
            byte[] actual = presented == null ? new byte[0] : presented.getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, actual)) {
                response.sendError(403, "Invalid service credential");
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
