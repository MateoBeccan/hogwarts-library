package com.hogwarts.library.controller;

import com.hogwarts.library.dto.auth.LoginResponse;
import com.hogwarts.library.security.DemoUserDetailsService;
import com.hogwarts.library.security.JwtService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("demo")
public class DemoAuthController {

    private final JwtService jwtService;
    private final DemoUserDetailsService demoUsers;

    public DemoAuthController(JwtService jwtService, DemoUserDetailsService demoUsers) {
        this.jwtService = jwtService;
        this.demoUsers = demoUsers;
    }

    @PostMapping("/api/auth/demo")
    public ResponseEntity<LoginResponse> login() {
        var visitante = demoUsers.loadUserByUsername(DemoUserDetailsService.USERNAME);
        var response = new LoginResponse(jwtService.generarToken(visitante), "Bearer",
                visitante.getUsername(), DemoUserDetailsService.ROLE);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
