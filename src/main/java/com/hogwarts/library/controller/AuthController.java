package com.hogwarts.library.controller;

import com.hogwarts.library.domain.Usuario;
import com.hogwarts.library.dto.auth.LoginRequest;
import com.hogwarts.library.dto.auth.LoginResponse;
import com.hogwarts.library.repository.UsuarioRepository;
import com.hogwarts.library.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("!demo")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            UsuarioRepository usuarioRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        request.username()
                );

        String token =
                jwtService.generarToken(userDetails);

        Usuario usuario =
                usuarioRepository
                        .findByUsername(request.username())
                        .orElseThrow();

        LoginResponse response =
                new LoginResponse(
                        token,
                        "Bearer",
                        usuario.getUsername(),
                        usuario.getRol().name()
                );

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
