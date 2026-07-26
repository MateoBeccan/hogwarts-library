package com.hogwarts.library.dto.alquiler;

import com.hogwarts.library.domain.enums.EstadoAlquiler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AlquilerResponse(

        Long id,

        Long clienteId,
        String clienteNombreCompleto,

        LocalDate fechaAlquiler,
        LocalDate fechaVencimiento,
        LocalDate fechaDevolucion,

        EstadoAlquiler estado,

        BigDecimal total,
        String observaciones,

        List<DetalleAlquilerResponse> detalles

) {
}