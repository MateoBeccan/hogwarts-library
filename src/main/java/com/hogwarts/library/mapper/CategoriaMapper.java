package com.hogwarts.library.mapper;

import com.hogwarts.library.domain.Categoria;
import com.hogwarts.library.dto.categoria.CategoriaRequest;
import com.hogwarts.library.dto.categoria.CategoriaResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public Categoria toEntity(CategoriaRequest request) {
        Categoria categoria = new Categoria();

        categoria.setNombre(request.nombre());
        categoria.setDescripcion(request.descripcion());
        categoria.setActiva(true);

        return categoria;
    }

    public CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getActiva()
        );
    }

    public void updateEntity(
            Categoria categoria,
            CategoriaRequest request
    ) {
        categoria.setNombre(request.nombre());
        categoria.setDescripcion(request.descripcion());
    }
}