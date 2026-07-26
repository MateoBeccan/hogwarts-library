package com.hogwarts.library.dto.ejemplar;

import com.hogwarts.library.domain.enums.EstadoEjemplar;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EjemplarResponse(

        Long id,
        String codigo,
        EstadoEjemplar estado,
        BigDecimal precioDia,
        LocalDate fechaAlta,
        String observaciones,

        Long libroId,
        String libroTitulo,
        String libroIsbn

) {
}