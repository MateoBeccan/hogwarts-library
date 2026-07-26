package com.hogwarts.library.dto.libro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LibroRequest(

        @NotBlank(message = "El ISBN es obligatorio")
        @Size(max = 20, message = "El ISBN no puede superar los 20 caracteres")
        String isbn,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede superar los 200 caracteres")
        String titulo,

        @NotBlank(message = "El autor es obligatorio")
        @Size(max = 150, message = "El autor no puede superar los 150 caracteres")
        String autor,

        @Size(max = 150, message = "La editorial no puede superar los 150 caracteres")
        String editorial,

        @Positive(message = "El año de publicación debe ser mayor a 0")
        Integer anioPublicacion,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion,

        @NotNull(message = "La categoría es obligatoria")
        @Positive(message = "El id de la categoría debe ser mayor a 0")
        Long categoriaId

) {
}