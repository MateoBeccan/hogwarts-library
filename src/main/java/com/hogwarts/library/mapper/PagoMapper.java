package com.hogwarts.library.mapper;

import com.hogwarts.library.domain.Alquiler;
import com.hogwarts.library.domain.Pago;
import com.hogwarts.library.dto.pago.PagoRequest;
import com.hogwarts.library.dto.pago.PagoResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PagoMapper {

    public Pago toEntity(
            PagoRequest request,
            Alquiler alquiler
    ) {

        Pago pago = new Pago();

        pago.setAlquiler(alquiler);
        pago.setMonto(request.monto());
        pago.setMetodoPago(request.metodoPago());
        pago.setObservaciones(
                normalizarNullable(request.observaciones())
        );

        return pago;
    }

    public PagoResponse toResponse(
            Pago pago,
            BigDecimal totalPagado
    ) {

        BigDecimal totalAlquiler =
                pago.getAlquiler().getTotal();

        BigDecimal saldoPendiente =
                totalAlquiler.subtract(totalPagado);

        return new PagoResponse(
                pago.getId(),
                pago.getAlquiler().getId(),
                pago.getFecha(),
                pago.getMonto(),
                pago.getMetodoPago(),
                pago.getObservaciones(),
                totalAlquiler,
                totalPagado,
                saldoPendiente
        );
    }

    private String normalizarNullable(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}