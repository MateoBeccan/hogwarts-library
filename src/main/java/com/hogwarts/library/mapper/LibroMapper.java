package com.hogwarts.library.mapper;

import com.hogwarts.library.domain.Autor;
import com.hogwarts.library.domain.Categoria;
import com.hogwarts.library.domain.Libro;
import com.hogwarts.library.dto.autor.AutorResponse;
import com.hogwarts.library.dto.libro.LibroRequest;
import com.hogwarts.library.dto.libro.LibroResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;

@Component
public class LibroMapper {

    public Libro toEntity(
            LibroRequest request,
            Categoria categoria,
            Set<Autor> autores
    ) {

        Libro libro = new Libro();

        libro.setIsbn(normalizar(request.isbn()));
        libro.setTitulo(normalizar(request.titulo()));
        libro.setEditorial(normalizarNullable(request.editorial()));
        libro.setAnioPublicacion(request.anioPublicacion());
        libro.setDescripcion(normalizarNullable(request.descripcion()));
        libro.setCategoria(categoria);
        libro.setAutores(autores);

        return libro;
    }

    public LibroResponse toResponse(Libro libro) {

        return new LibroResponse(
                libro.getId(),
                libro.getIsbn(),
                libro.getTitulo(),
                libro.getAutores()
                        .stream()
                        .sorted(Comparator.comparing(Autor::getId))
                        .map(autor -> new AutorResponse(
                                autor.getId(),
                                autor.getNombreCompleto(),
                                autor.getActivo()
                        ))
                        .toList(),
                libro.getEditorial(),
                libro.getAnioPublicacion(),
                libro.getDescripcion(),
                libro.getActivo(),
                libro.getCategoria().getId(),
                libro.getCategoria().getNombre()
        );
    }

    public void updateEntity(
            Libro libro,
            LibroRequest request,
            Categoria categoria,
            Set<Autor> autores
    ) {

        libro.setIsbn(normalizar(request.isbn()));
        libro.setTitulo(normalizar(request.titulo()));
        libro.setEditorial(normalizarNullable(request.editorial()));
        libro.setAnioPublicacion(request.anioPublicacion());
        libro.setDescripcion(normalizarNullable(request.descripcion()));
        libro.setCategoria(categoria);
        libro.getAutores().clear();
        libro.getAutores().addAll(autores);
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