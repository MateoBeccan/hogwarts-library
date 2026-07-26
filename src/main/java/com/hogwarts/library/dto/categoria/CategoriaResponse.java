package com.hogwarts.library.dto.categoria;

public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        Boolean activa
) {
}