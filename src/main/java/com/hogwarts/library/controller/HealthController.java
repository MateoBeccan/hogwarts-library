package com.hogwarts.library.controller;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final ApplicationAvailability availability;
    private final JdbcTemplate jdbc;

    public HealthController(ApplicationAvailability availability, JdbcTemplate jdbc) {
        this.availability = availability;
        this.jdbc = jdbc;
    }

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, String>> health() {
        boolean ready = availability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC;
        if (ready) {
            try {
                jdbc.queryForObject("SELECT 1", Integer.class);
            } catch (DataAccessException ex) {
                ready = false;
            }
        }

        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("status", ready ? "UP" : "DOWN"));
    }
}
