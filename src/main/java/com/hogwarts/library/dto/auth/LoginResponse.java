package com.hogwarts.library.dto.auth;

public record LoginResponse(

        String token,
        String type,
        String username,
        String rol

) {
}