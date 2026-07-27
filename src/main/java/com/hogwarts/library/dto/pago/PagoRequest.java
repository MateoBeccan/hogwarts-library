package com.hogwarts.library.dto.pago;

import com.hogwarts.library.domain.enums.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PagoRequest(

        @NotNull(message = "El alquiler es obligatorio")
        @Positive(message = "El id del alquiler debe ser mayor a 0")
        Long alquilerId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El monto debe ser mayor a 0"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "El monto debe tener como máximo 10 enteros y 2 decimales"
        )
        BigDecimal monto,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        @Size(
                max = 500,
                message = "Las observaciones no pueden superar los 500 caracteres"
        )
        String observaciones

) {
}