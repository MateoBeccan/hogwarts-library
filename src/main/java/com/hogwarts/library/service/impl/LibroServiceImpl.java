package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Categoria;
import com.hogwarts.library.domain.Libro;
import com.hogwarts.library.dto.libro.LibroRequest;
import com.hogwarts.library.dto.libro.LibroResponse;
import com.hogwarts.library.exception.BusinessException;
import com.hogwarts.library.exception.DuplicateResourceException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.LibroMapper;
import com.hogwarts.library.repository.CategoriaRepository;
import com.hogwarts.library.repository.LibroRepository;
import com.hogwarts.library.service.LibroService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LibroServiceImpl implements LibroService {

    private final LibroRepository libroRepository;
    private final CategoriaRepository categoriaRepository;
    private final LibroMapper libroMapper;

    public LibroServiceImpl(
            LibroRepository libroRepository,
            CategoriaRepository categoriaRepository,
            LibroMapper libroMapper
    ) {
        this.libroRepository = libroRepository;
        this.categoriaRepository = categoriaRepository;
        this.libroMapper = libroMapper;
    }

    @Override
    public LibroResponse crear(LibroRequest request) {

        String isbn = request.isbn().trim();

        if (libroRepository.existsByIsbn(isbn)) {
            throw new DuplicateResourceException(
                    "Ya existe un libro con ISBN: " + isbn
            );
        }

        Categoria categoria = buscarCategoriaPorId(request.categoriaId());

        validarCategoriaActiva(categoria);

        Libro libro = libroMapper.toEntity(request, categoria);

        libro.setActivo(true);

        Libro guardado = libroRepository.save(libro);

        return libroMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public LibroResponse buscarPorId(Long id) {

        Libro libro = buscarEntidadPorId(id);

        return libroMapper.toResponse(libro);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LibroResponse> listar() {

        return libroRepository.findAll()
                .stream()
                .map(libroMapper::toResponse)
                .toList();
    }

    @Override
    public LibroResponse actualizar(
            Long id,
            LibroRequest request
    ) {

        Libro libro = buscarEntidadPorId(id);

        String isbn = request.isbn().trim();

        if (libroRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new DuplicateResourceException(
                    "Ya existe otro libro con ISBN: " + isbn
            );
        }

        Categoria categoria = buscarCategoriaPorId(request.categoriaId());

        validarCategoriaActiva(categoria);

        libroMapper.updateEntity(
                libro,
                request,
                categoria
        );

        Libro actualizado = libroRepository.save(libro);

        return libroMapper.toResponse(actualizado);
    }

    @Override
    public void desactivar(Long id) {

        Libro libro = buscarEntidadPorId(id);

        libro.setActivo(false);

        libroRepository.save(libro);
    }

    @Override
    public LibroResponse reactivar(Long id) {

        Libro libro = buscarEntidadPorId(id);

        validarCategoriaActiva(libro.getCategoria());

        libro.setActivo(true);

        Libro actualizado = libroRepository.save(libro);

        return libroMapper.toResponse(actualizado);
    }

    private Libro buscarEntidadPorId(Long id) {

        return libroRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un libro con id: " + id
                        )
                );
    }

    private Categoria buscarCategoriaPorId(Long id) {

        return categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe una categoría con id: " + id
                        )
                );
    }

    private void validarCategoriaActiva(Categoria categoria) {

        if (!Boolean.TRUE.equals(categoria.getActiva())) {
            throw new BusinessException(
                    "No se puede asociar un libro a una categoría inactiva"
            );
        }
    }
}