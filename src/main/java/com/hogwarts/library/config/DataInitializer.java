package com.hogwarts.library.config;

import com.hogwarts.library.domain.Usuario;
import com.hogwarts.library.domain.enums.RolUsuario;
import com.hogwarts.library.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataInitializer {

    private static final Logger log =
            LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner crearUsuariosIniciales(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {

        return args -> {

            if (!usuarioRepository.existsByUsername("admin")) {

                Usuario admin = new Usuario();

                admin.setUsername("admin");
                admin.setPassword(
                        passwordEncoder.encode("Admin123!")
                );
                admin.setNombre("Administrador");
                admin.setApellido("Hogwarts");
                admin.setRol(RolUsuario.ADMIN);
                admin.setActivo(true);

                usuarioRepository.save(admin);

                log.info(
                        "Usuario administrador inicial creado"
                );
            }

            if (!usuarioRepository.existsByUsername("empleado")) {

                Usuario empleado = new Usuario();

                empleado.setUsername("empleado");
                empleado.setPassword(
                        passwordEncoder.encode("Empleado123!")
                );
                empleado.setNombre("Empleado");
                empleado.setApellido("Hogwarts");
                empleado.setRol(RolUsuario.EMPLEADO);
                empleado.setActivo(true);

                usuarioRepository.save(empleado);

                log.info(
                        "Usuario empleado inicial creado"
                );
            }
        };
    }
}