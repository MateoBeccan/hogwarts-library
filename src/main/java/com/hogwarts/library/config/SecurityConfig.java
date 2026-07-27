package com.hogwarts.library.config;

import com.hogwarts.library.security.CustomAccessDeniedHandler;
import com.hogwarts.library.security.CustomAuthenticationEntryPoint;
import com.hogwarts.library.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // API REST + JWT: no necesitamos CSRF
                .csrf(csrf -> csrf.disable())

                // No mantenemos sesión en el servidor.
                // Cada request debe traer su JWT.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Respuestas personalizadas para errores de seguridad
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth

                        // =========================================
                        // ENDPOINTS PÚBLICOS
                        // =========================================

                        .requestMatchers("/api/auth/**")
                        .permitAll()


                        // =========================================
                        // ADMINISTRACIÓN DE USUARIOS
                        // =========================================

                        .requestMatchers("/api/usuarios/**")
                        .hasRole("ADMIN")


                        // =========================================
                        // OPERACIONES SOLO ADMIN
                        // =========================================

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/categorias/**",
                                "/api/clientes/**",
                                "/api/libros/**"
                        )
                        .hasRole("ADMIN")


                        // =========================================
                        // OPERACIONES DE LA BIBLIOTECA
                        // ADMIN y EMPLEADO
                        // =========================================

                        .requestMatchers(
                                "/api/categorias/**",
                                "/api/clientes/**",
                                "/api/libros/**",
                                "/api/ejemplares/**",
                                "/api/alquileres/**",
                                "/api/pagos/**"
                        )
                        .hasAnyRole("ADMIN", "EMPLEADO")


                        // Todo lo demás requiere autenticación
                        .anyRequest()
                        .authenticated()
                )

                // El JWT se procesa antes del filtro estándar
                // de username/password de Spring Security.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }
}