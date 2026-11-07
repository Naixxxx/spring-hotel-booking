package io.staypilot.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

@SpringBootTest(properties = {"eureka.client.enabled=false"})
@AutoConfigureMockMvc
class CatalogSecurityMvcTest {

    @Autowired
    MockMvc mock;

    @Test
    void unauthenticatedCatalogAccessIsDenied() throws Exception {
        mock.perform(get("/api/hotels")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateHotel() throws Exception {
        mock.perform(post("/api/hotels").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType("application/json").content("{\"name\":\"Test\",\"address\":\"Street\",\"city\":\"Berlin\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void internalApiRequiresPrivateCredential() throws Exception {
        mock.perform(post("/internal/rooms/1/hold").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }
}
