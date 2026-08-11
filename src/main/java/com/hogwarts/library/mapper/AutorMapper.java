package com.hogwarts.library.mapper;

import com.hogwarts.library.domain.Autor;
import com.hogwarts.library.dto.autor.AutorRequest;
import com.hogwarts.library.dto.autor.AutorResponse;
import org.springframework.stereotype.Component;

@Component
public class AutorMapper {

    public Autor toEntity(AutorRequest request) {
        Autor autor = new Autor();
        autor.setNombreCompleto(normalizar(request.nombreCompleto()));
        autor.setActivo(true);
        return autor;
    }

    public AutorResponse toResponse(Autor autor) {
        return new AutorResponse(
                autor.getId(),
                autor.getNombreCompleto(),
                autor.getActivo()
        );
    }

    public void updateEntity(Autor autor, AutorRequest request) {
        autor.setNombreCompleto(normalizar(request.nombreCompleto()));
    }

    private String normalizar(String valor) {
        return valor.trim();
    }
}