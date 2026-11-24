package com.hogwarts.library.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.cors.allowed-origins=https://demo.example.invalid")
@AutoConfigureMockMvc
@ActiveProfiles({"demo", "test"})
class DemoSecurityIntegrationTest {

    private static final String ORIGIN = "https://demo.example.invalid";

    @Autowired
    private MockMvc mvc;

    @Test
    void issuesJwtWithoutPasswordAndRequiresItForReads() throws Exception {
        mvc.perform(get("/api/libros")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/libros").header(HttpHeaders.AUTHORIZATION, demoBearer()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login")).andExpect(status().isUnauthorized());
        mvc.perform(get("/healthz"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @ParameterizedTest
    @CsvSource({
            "POST, /api/libros", "PUT, /api/autores/1", "DELETE, /api/clientes/1",
            "PATCH, /api/libros/1/reactivar", "PATCH, /api/ejemplares/1/fuera-servicio",
            "POST, /api/alquileres", "PATCH, /api/alquileres/1/finalizar",
            "PATCH, /api/alquileres/1/cancelar", "POST, /api/pagos",
            "GET, /api/usuarios", "GET, /actuator/env", "GET, /v3/api-docs"
    })
    void rejectsWritesAndAdministrativeRoutes(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path).header(HttpHeaders.AUTHORIZATION, demoBearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsAllowsOnlyTheConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/libros")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN));

        mvc.perform(options("/api/libros")
                        .header(HttpHeaders.ORIGIN, "https://other.example.invalid")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private String demoBearer() throws Exception {
        String response = mvc.perform(post("/api/auth/demo"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.rol").value("DEMO"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.<String>read(response, "$.token");
    }
}
