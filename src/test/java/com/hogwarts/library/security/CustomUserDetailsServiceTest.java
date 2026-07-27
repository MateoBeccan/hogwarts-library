package com.hogwarts.library.security;

import com.hogwarts.library.domain.Usuario;
import com.hogwarts.library.domain.enums.RolUsuario;
import com.hogwarts.library.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {

        userDetailsService =
                new CustomUserDetailsService(usuarioRepository);
    }

    @Test
    void loadUserByUsername_deberiaCargarAdminConRolCorrecto() {

        // Arrange
        Usuario usuario = crearUsuario(
                1L,
                "admin",
                "$2a$10$passwordHash",
                RolUsuario.ADMIN,
                true
        );

        when(usuarioRepository.findByUsername("admin"))
                .thenReturn(Optional.of(usuario));

        // Act
        UserDetails resultado =
                userDetailsService.loadUserByUsername("admin");

        // Assert
        assertNotNull(resultado);

        assertEquals(
                "admin",
                resultado.getUsername()
        );

        assertEquals(
                "$2a$10$passwordHash",
                resultado.getPassword()
        );

        assertTrue(resultado.isEnabled());

        assertTrue(
                resultado.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_ADMIN")
                        )
        );

        verify(usuarioRepository)
                .findByUsername("admin");
    }

    @Test
    void loadUserByUsername_deberiaCargarEmpleadoConRolCorrecto() {

        // Arrange
        Usuario usuario = crearUsuario(
                2L,
                "empleado",
                "$2a$10$passwordHash",
                RolUsuario.EMPLEADO,
                true
        );

        when(usuarioRepository.findByUsername("empleado"))
                .thenReturn(Optional.of(usuario));

        // Act
        UserDetails resultado =
                userDetailsService.loadUserByUsername("empleado");

        // Assert
        assertNotNull(resultado);

        assertEquals(
                "empleado",
                resultado.getUsername()
        );

        assertTrue(resultado.isEnabled());

        assertTrue(
                resultado.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_EMPLEADO")
                        )
        );

        verify(usuarioRepository)
                .findByUsername("empleado");
    }

    @Test
    void loadUserByUsername_deberiaMarcarUsuarioComoDeshabilitadoCuandoEstaInactivo() {

        // Arrange
        Usuario usuario = crearUsuario(
                3L,
                "inactivo",
                "$2a$10$passwordHash",
                RolUsuario.EMPLEADO,
                false
        );

        when(usuarioRepository.findByUsername("inactivo"))
                .thenReturn(Optional.of(usuario));

        // Act
        UserDetails resultado =
                userDetailsService.loadUserByUsername("inactivo");

        // Assert
        assertNotNull(resultado);

        assertEquals(
                "inactivo",
                resultado.getUsername()
        );

        assertFalse(resultado.isEnabled());

        verify(usuarioRepository)
                .findByUsername("inactivo");
    }

    @Test
    void loadUserByUsername_deberiaLanzarExcepcionCuandoUsuarioNoExiste() {

        // Arrange
        when(usuarioRepository.findByUsername("desconocido"))
                .thenReturn(Optional.empty());

        // Act + Assert
        UsernameNotFoundException exception =
                assertThrows(
                        UsernameNotFoundException.class,
                        () ->
                                userDetailsService
                                        .loadUserByUsername("desconocido")
                );

        assertEquals(
                "Usuario no encontrado",
                exception.getMessage()
        );

        verify(usuarioRepository)
                .findByUsername("desconocido");
    }

    private Usuario crearUsuario(
            Long id,
            String username,
            String password,
            RolUsuario rol,
            boolean activo
    ) {

        Usuario usuario = new Usuario();

        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPassword(password);
        usuario.setNombre("Usuario");
        usuario.setApellido("Prueba");
        usuario.setRol(rol);
        usuario.setActivo(activo);

        return usuario;
    }
}