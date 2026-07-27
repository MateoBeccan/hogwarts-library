package com.hogwarts.library.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public String generarToken(UserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        claims.put(
                "roles",
                userDetails.getAuthorities()
        );

        return generarToken(
                claims,
                userDetails
        );
    }

    private String generarToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {

        long ahora = System.currentTimeMillis();

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + expirationMs))
                .signWith(obtenerClave())
                .compact();
    }

    public String extraerUsername(String token) {

        return extraerClaim(
                token,
                Claims::getSubject
        );
    }

    public boolean esTokenValido(
            String token,
            UserDetails userDetails
    ) {

        String username = extraerUsername(token);

        return username.equals(userDetails.getUsername())
                && !estaExpirado(token);
    }

    private boolean estaExpirado(String token) {

        return extraerExpiracion(token)
                .before(new Date());
    }

    private Date extraerExpiracion(String token) {

        return extraerClaim(
                token,
                Claims::getExpiration
        );
    }

    private <T> T extraerClaim(
            String token,
            Function<Claims, T> resolver
    ) {

        Claims claims = extraerTodosLosClaims(token);

        return resolver.apply(claims);
    }

    private Claims extraerTodosLosClaims(String token) {

        return Jwts.parser()
                .verifyWith(obtenerClave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey obtenerClave() {

        byte[] keyBytes =
                secret.getBytes();

        return Keys.hmacShaKeyFor(keyBytes);
    }
}