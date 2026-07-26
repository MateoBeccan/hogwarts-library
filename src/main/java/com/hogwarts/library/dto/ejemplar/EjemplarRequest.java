package com.hogwarts.library.dto.ejemplar;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EjemplarRequest(

        @NotBlank(message = "El código del ejemplar es obligatorio")
        @Size(max = 30, message = "El código no puede superar los 30 caracteres")
        String codigo,

        @NotNull(message = "El precio por día es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El precio por día debe ser mayor a 0"
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "El precio por día debe tener como máximo 8 enteros y 2 decimales"
        )
        BigDecimal precioDia,

        @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
        String observaciones,

        @NotNull(message = "El libro es obligatorio")
        @Positive(message = "El id del libro debe ser mayor a 0")
        Long libroId

) {
}