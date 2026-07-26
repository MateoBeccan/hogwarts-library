package com.hogwarts.library.mapper;

import com.hogwarts.library.domain.Alquiler;
import com.hogwarts.library.domain.DetalleAlquiler;
import com.hogwarts.library.dto.alquiler.AlquilerResponse;
import com.hogwarts.library.dto.alquiler.DetalleAlquilerResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlquilerMapper {

    public AlquilerResponse toResponse(Alquiler alquiler) {

        List<DetalleAlquilerResponse> detalles = alquiler.getDetalles()
                .stream()
                .map(this::toDetalleResponse)
                .toList();

        return new AlquilerResponse(
                alquiler.getId(),
                alquiler.getCliente().getId(),
                alquiler.getCliente().getNombre() + " " + alquiler.getCliente().getApellido(),
                alquiler.getFechaAlquiler(),
                alquiler.getFechaVencimiento(),
                alquiler.getFechaDevolucion(),
                alquiler.getEstado(),
                alquiler.getTotal(),
                alquiler.getObservaciones(),
                detalles
        );
    }

    private DetalleAlquilerResponse toDetalleResponse(
            DetalleAlquiler detalle
    ) {

        return new DetalleAlquilerResponse(
                detalle.getId(),

                detalle.getEjemplar().getId(),
                detalle.getEjemplar().getCodigo(),

                detalle.getEjemplar().getLibro().getId(),
                detalle.getEjemplar().getLibro().getTitulo(),

                detalle.getPrecioDia(),
                detalle.getCantidadDias(),
                detalle.getSubtotal()
        );
    }
}