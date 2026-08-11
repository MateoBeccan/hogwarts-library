package com.hogwarts.library.dto.autor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AutorRequest(

        @NotBlank(message = "El nombre completo del autor es obligatorio")
        @Size(max = 150, message = "El nombre completo del autor no puede superar los 150 caracteres")
        String nombreCompleto

) {
}