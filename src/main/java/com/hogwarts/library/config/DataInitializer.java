package com.hogwarts.library.config;

import com.hogwarts.library.domain.Usuario;
import com.hogwarts.library.domain.enums.RolUsuario;
import com.hogwarts.library.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

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

                System.out.println(
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

                System.out.println(
                        "Usuario empleado inicial creado"
                );
            }
        };
    }
}