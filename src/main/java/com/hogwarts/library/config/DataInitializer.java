package com.hogwarts.library.config;

import com.hogwarts.library.domain.Usuario;
import com.hogwarts.library.domain.enums.RolUsuario;
import com.hogwarts.library.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Profile("dev & !hosted & !test & !demo & !prod")
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String empleadoPassword;

    public DataInitializer(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                           @Value("${DEV_ADMIN_PASSWORD:}") String adminPassword,
                           @Value("${DEV_EMPLEADO_PASSWORD:}") String empleadoPassword) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.empleadoPassword = empleadoPassword;
    }

    @Override
    public void run(String... args) {
        crearSiFalta("admin", RolUsuario.ADMIN, adminPassword);
        crearSiFalta("empleado", RolUsuario.EMPLEADO, empleadoPassword);
    }

    private void crearSiFalta(String username, RolUsuario rol, String password) {
        if (password.isBlank() || usuarios.existsByUsername(username)) {
            return;
        }
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña inicial de desarrollo debe tener al menos 12 caracteres y hasta 72 bytes");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setNombre(username);
        usuario.setApellido("Hogwarts");
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuarios.save(usuario);
    }
}
