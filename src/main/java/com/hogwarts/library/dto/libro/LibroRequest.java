package com.hogwarts.library.dto.libro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LibroRequest(

        @NotBlank(message = "El ISBN es obligatorio")
        @Size(max = 20, message = "El ISBN no puede superar los 20 caracteres")
        String isbn,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede superar los 200 caracteres")
        String titulo,

        @NotEmpty(message = "Debe seleccionar al menos un autor")
        List<@NotNull(message = "El id del autor es obligatorio")
        @Positive(message = "El id del autor debe ser mayor a 0") Long> autorIds,

        @Size(max = 150, message = "La editorial no puede superar los 150 caracteres")
        String editorial,

        @Positive(message = "El anio de publicacion debe ser mayor a 0")
        Integer anioPublicacion,

        @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
        String descripcion,

        @NotNull(message = "La categoria es obligatoria")
        @Positive(message = "El id de la categoria debe ser mayor a 0")
        Long categoriaId

) {
}