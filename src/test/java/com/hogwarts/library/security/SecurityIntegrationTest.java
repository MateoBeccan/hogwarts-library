package com.hogwarts.library.security;

import com.hogwarts.library.domain.Usuario;
import com.hogwarts.library.domain.enums.RolUsuario;
import com.hogwarts.library.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String empleadoToken;

    @BeforeEach
    void setUp() {

        Usuario admin = usuarioRepository
                .findByUsername("admin")
                .orElseGet(() -> {

                    Usuario usuario = new Usuario();
                    usuario.setUsername("admin");
                    usuario.setPassword(
                            passwordEncoder.encode("Admin123!")
                    );
                    usuario.setNombre("Administrador");
                    usuario.setApellido("Test");
                    usuario.setRol(RolUsuario.ADMIN);
                    usuario.setActivo(true);

                    return usuarioRepository.save(usuario);
                });

        Usuario empleado = usuarioRepository
                .findByUsername("empleado")
                .orElseGet(() -> {

                    Usuario usuario = new Usuario();
                    usuario.setUsername("empleado");
                    usuario.setPassword(
                            passwordEncoder.encode("Empleado123!")
                    );
                    usuario.setNombre("Empleado");
                    usuario.setApellido("Test");
                    usuario.setRol(RolUsuario.EMPLEADO);
                    usuario.setActivo(true);

                    return usuarioRepository.save(usuario);
                });

        adminToken = jwtService.generarToken(
                new org.springframework.security.core.userdetails.User(
                        admin.getUsername(),
                        admin.getPassword(),
                        admin.getActivo(),
                        true,
                        true,
                        true,
                        java.util.List.of(
                                new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                )
        );

        empleadoToken = jwtService.generarToken(
                new org.springframework.security.core.userdetails.User(
                        empleado.getUsername(),
                        empleado.getPassword(),
                        empleado.getActivo(),
                        true,
                        true,
                        true,
                        java.util.List.of(
                                new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority("ROLE_EMPLEADO")
                        )
                )
        );
    }

    @Test
    void clientes_sinToken_deberiaRetornar401() throws Exception {

        mockMvc.perform(
                        get("/api/clientes")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/json"
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void clientes_conTokenEmpleado_deberiaRetornar200() throws Exception {

        mockMvc.perform(
                        get("/api/clientes")
                                .header(
                                        "Authorization",
                                        "Bearer " + empleadoToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void eliminarCliente_conTokenEmpleado_deberiaRetornar403() throws Exception {

        mockMvc.perform(
                        delete("/api/clientes/999999")
                                .header(
                                        "Authorization",
                                        "Bearer " + empleadoToken
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/json"
                ))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void eliminarCliente_conTokenAdmin_deberiaSuperarSeguridad() throws Exception {

        mockMvc.perform(
                        delete("/api/clientes/999999")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void reactivarCliente_conTokenEmpleado_deberiaRetornar403()
            throws Exception {

        mockMvc.perform(
                        patch("/api/clientes/999999/reactivar")
                                .header(
                                        "Authorization",
                                        "Bearer " + empleadoToken
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void reactivarCliente_conTokenAdmin_deberiaSuperarSeguridad()
            throws Exception {

        mockMvc.perform(
                        patch("/api/clientes/999999/reactivar")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isNotFound());
    }

}