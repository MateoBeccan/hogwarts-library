package com.hogwarts.library.dto.libro;

import com.hogwarts.library.dto.autor.AutorResponse;

import java.util.List;

public record LibroResponse(

        Long id,
        String isbn,
        String titulo,
        List<AutorResponse> autores,
        String editorial,
        Integer anioPublicacion,
        String descripcion,
        Boolean activo,
        Long categoriaId,
        String categoriaNombre

) {
}