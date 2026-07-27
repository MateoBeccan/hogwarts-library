package com.hogwarts.library.controller;

import com.hogwarts.library.domain.Usuario;
import com.hogwarts.library.domain.enums.RolUsuario;
import com.hogwarts.library.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {

        usuarioRepository.deleteAll();

        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setPassword(
                passwordEncoder.encode("Admin123!")
        );
        admin.setNombre("Administrador");
        admin.setApellido("Test");
        admin.setRol(RolUsuario.ADMIN);
        admin.setActivo(true);

        usuarioRepository.save(admin);

        Usuario empleado = new Usuario();
        empleado.setUsername("empleado");
        empleado.setPassword(
                passwordEncoder.encode("Empleado123!")
        );
        empleado.setNombre("Empleado");
        empleado.setApellido("Test");
        empleado.setRol(RolUsuario.EMPLEADO);
        empleado.setActivo(true);

        usuarioRepository.save(empleado);
    }

    @Test
    void login_deberiaRetornarTokenCuandoAdminTieneCredencialesValidas()
            throws Exception {

        String body = """
                {
                  "username": "admin",
                  "password": "Admin123!"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void login_deberiaRetornarTokenCuandoEmpleadoTieneCredencialesValidas()
            throws Exception {

        String body = """
                {
                  "username": "empleado",
                  "password": "Empleado123!"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("empleado"))
                .andExpect(jsonPath("$.rol").value("EMPLEADO"));
    }

    @Test
    void login_deberiaRetornar401CuandoPasswordEsIncorrecto()
            throws Exception {

        String body = """
                {
                  "username": "admin",
                  "password": "incorrecta"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(
                        jsonPath("$.message")
                                .value("Usuario o contraseña incorrectos")
                );
    }

    @Test
    void login_deberiaRetornar400CuandoUsernameEstaVacio()
            throws Exception {

        String body = """
                {
                  "username": "",
                  "password": "Admin123!"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}