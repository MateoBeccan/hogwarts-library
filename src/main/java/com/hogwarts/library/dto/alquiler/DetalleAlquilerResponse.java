package com.hogwarts.library.dto.alquiler;

import java.math.BigDecimal;

public record DetalleAlquilerResponse(

        Long id,

        Long ejemplarId,
        String ejemplarCodigo,

        Long libroId,
        String libroTitulo,

        BigDecimal precioDia,
        Integer cantidadDias,
        BigDecimal subtotal

) {
}