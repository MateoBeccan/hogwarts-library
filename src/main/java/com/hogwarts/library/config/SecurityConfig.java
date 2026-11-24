package com.hogwarts.library.config;

import com.hogwarts.library.security.CustomAccessDeniedHandler;
import com.hogwarts.library.security.CustomAuthenticationEntryPoint;
import com.hogwarts.library.security.DemoUserDetailsService;
import com.hogwarts.library.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            CustomAuthenticationEntryPoint authenticationEntryPoint, CustomAccessDeniedHandler accessDeniedHandler,
            UrlBasedCorsConfigurationSource cors, Environment environment
    ) throws Exception {
        boolean demo = environment.matchesProfiles("demo");
        http.cors(config -> config.configurationSource(cors))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                            .requestMatchers(HttpMethod.GET, "/healthz").permitAll();
                    if (demo) {
                        auth.requestMatchers(HttpMethod.POST, "/api/auth/demo").permitAll()
                                .requestMatchers(HttpMethod.GET,
                                        "/api/categorias", "/api/categorias/{id:[0-9]+}",
                                        "/api/autores", "/api/autores/{id:[0-9]+}",
                                        "/api/libros", "/api/libros/{id:[0-9]+}",
                                        "/api/ejemplares", "/api/ejemplares/{id:[0-9]+}",
                                        "/api/clientes", "/api/clientes/{id:[0-9]+}",
                                        "/api/alquileres", "/api/alquileres/{id:[0-9]+}",
                                        "/api/alquileres/cliente/{id:[0-9]+}",
                                        "/api/pagos/{id:[0-9]+}", "/api/pagos/alquiler/{id:[0-9]+}")
                                .hasRole(DemoUserDetailsService.ROLE);
                    } else {
                        auth.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/categorias/**", "/api/autores/**",
                                        "/api/clientes/**", "/api/libros/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/clientes/*/reactivar",
                                        "/api/libros/*/reactivar").hasRole("ADMIN")
                                .requestMatchers("/api/categorias/**", "/api/autores/**", "/api/clientes/**",
                                        "/api/libros/**", "/api/ejemplares/**", "/api/alquileres/**", "/api/pagos/**")
                                .hasAnyRole("ADMIN", "EMPLEADO");
                    }
                    auth.anyRequest().denyAll();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String configuredOrigins, Environment environment
    ) {
        boolean hosted = environment.matchesProfiles("hosted", "demo", "prod");
        List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (hosted && origins.isEmpty()) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS debe incluir los orígenes del frontend");
        }
        for (String origin : origins) {
            URI uri = URI.create(origin);
            boolean validScheme = "https".equals(uri.getScheme()) || (!hosted && "http".equals(uri.getScheme()));
            if (!validScheme || uri.getHost() == null || uri.getUserInfo() != null
                    || !uri.getRawPath().isEmpty() || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS debe contener orígenes exactos sin rutas; HTTPS es obligatorio en hosting");
            }
        }

        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(environment.matchesProfiles("demo")
                ? List.of("GET", "POST")
                : List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile("!demo")
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
