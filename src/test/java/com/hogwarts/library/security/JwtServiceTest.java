package com.hogwarts.library.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails admin;

    @BeforeEach
    void setUp() {

        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                "hogwarts-library-jwt-secret-key-2026-development"
        );

        ReflectionTestUtils.setField(
                jwtService,
                "expirationMs",
                3_600_000L
        );

        admin = User.builder()
                .username("admin")
                .password("irrelevante")
                .roles("ADMIN")
                .build();
    }

    @Test
    void generarToken_deberiaGenerarTokenValido() {

        String token = jwtService.generarToken(admin);

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertTrue(
                jwtService.esTokenValido(token, admin)
        );
    }

    @Test
    void extraerUsername_deberiaRetornarUsernameDelToken() {

        String token = jwtService.generarToken(admin);

        String username =
                jwtService.extraerUsername(token);

        assertEquals(
                "admin",
                username
        );
    }

    @Test
    void esTokenValido_deberiaRetornarFalseCuandoUsuarioNoCoincide() {

        String token = jwtService.generarToken(admin);

        UserDetails otroUsuario =
                User.builder()
                        .username("empleado")
                        .password("irrelevante")
                        .roles("EMPLEADO")
                        .build();

        boolean valido =
                jwtService.esTokenValido(
                        token,
                        otroUsuario
                );

        assertFalse(valido);
    }
    @Test
    void esTokenValido_deberiaRechazarTokenExpirado() throws InterruptedException {

        ReflectionTestUtils.setField(
                jwtService,
                "expirationMs",
                1L
        );

        String token =
                jwtService.generarToken(admin);

        Thread.sleep(10);

        assertThrows(
                Exception.class,
                () -> jwtService.esTokenValido(
                        token,
                        admin
                )
        );
    }
    @Test
    void extraerUsername_deberiaRechazarTokenManipulado() {

        String token =
                jwtService.generarToken(admin);

        String tokenManipulado =
                token.substring(
                        0,
                        token.length() - 2
                ) + "xx";

        assertThrows(
                Exception.class,
                () -> jwtService.extraerUsername(
                        tokenManipulado
                )
        );
    }

}