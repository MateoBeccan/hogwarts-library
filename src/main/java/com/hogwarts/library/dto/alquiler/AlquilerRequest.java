package com.hogwarts.library.dto.alquiler;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record AlquilerRequest(

        @NotNull(message = "El cliente es obligatorio")
        @Positive(message = "El id del cliente debe ser mayor a 0")
        Long clienteId,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        @Future(message = "La fecha de vencimiento debe ser futura")
        LocalDate fechaVencimiento,

        @NotEmpty(message = "Debe seleccionar al menos un ejemplar")
        @Size(
                max = 3,
                message = "Un alquiler no puede contener más de 3 ejemplares"
        )
        List<@Positive(message = "El id del ejemplar debe ser mayor a 0") Long> ejemplarIds,

        @Size(
                max = 500,
                message = "Las observaciones no pueden superar los 500 caracteres"
        )
        String observaciones

) {
}