package com.hogwarts.library.dto.libro;

public record LibroResponse(

        Long id,
        String isbn,
        String titulo,
        String autor,
        String editorial,
        Integer anioPublicacion,
        String descripcion,
        Boolean activo,
        Long categoriaId,
        String categoriaNombre

) {
}