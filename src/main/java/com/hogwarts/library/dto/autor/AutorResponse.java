package com.hogwarts.library.dto.autor;

public record AutorResponse(
        Long id,
        String nombreCompleto,
        Boolean activo
) {
}