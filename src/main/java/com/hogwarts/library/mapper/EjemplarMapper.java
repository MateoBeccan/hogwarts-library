package com.hogwarts.library.mapper;

import com.hogwarts.library.domain.Ejemplar;
import com.hogwarts.library.domain.Libro;
import com.hogwarts.library.dto.ejemplar.EjemplarRequest;
import com.hogwarts.library.dto.ejemplar.EjemplarResponse;
import org.springframework.stereotype.Component;

@Component
public class EjemplarMapper {

    public Ejemplar toEntity(
            EjemplarRequest request,
            Libro libro
    ) {

        Ejemplar ejemplar = new Ejemplar();

        ejemplar.setCodigo(normalizar(request.codigo()));
        ejemplar.setPrecioDia(request.precioDia());
        ejemplar.setObservaciones(
                normalizarNullable(request.observaciones())
        );
        ejemplar.setLibro(libro);

        return ejemplar;
    }

    public EjemplarResponse toResponse(Ejemplar ejemplar) {

        return new EjemplarResponse(
                ejemplar.getId(),
                ejemplar.getCodigo(),
                ejemplar.getEstado(),
                ejemplar.getPrecioDia(),
                ejemplar.getFechaAlta(),
                ejemplar.getObservaciones(),

                ejemplar.getLibro().getId(),
                ejemplar.getLibro().getTitulo(),
                ejemplar.getLibro().getIsbn()
        );
    }

    public void updateEntity(
            Ejemplar ejemplar,
            EjemplarRequest request,
            Libro libro
    ) {

        ejemplar.setCodigo(normalizar(request.codigo()));
        ejemplar.setPrecioDia(request.precioDia());
        ejemplar.setObservaciones(
                normalizarNullable(request.observaciones())
        );
        ejemplar.setLibro(libro);
    }

    private String normalizar(String valor) {
        return valor.trim();
    }

    private String normalizarNullable(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}