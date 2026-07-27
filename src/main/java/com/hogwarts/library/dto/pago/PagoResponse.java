package com.hogwarts.library.dto.pago;

import com.hogwarts.library.domain.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(

        Long id,
        Long alquilerId,

        LocalDateTime fecha,

        BigDecimal monto,
        MetodoPago metodoPago,
        String observaciones,

        BigDecimal totalAlquiler,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente

) {
}