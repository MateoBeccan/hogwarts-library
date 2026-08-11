package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Autor;
import com.hogwarts.library.domain.Categoria;
import com.hogwarts.library.domain.Libro;
import com.hogwarts.library.dto.libro.LibroRequest;
import com.hogwarts.library.dto.libro.LibroResponse;
import com.hogwarts.library.exception.BusinessException;
import com.hogwarts.library.exception.DuplicateResourceException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.LibroMapper;
import com.hogwarts.library.repository.AutorRepository;
import com.hogwarts.library.repository.CategoriaRepository;
import com.hogwarts.library.repository.LibroRepository;
import com.hogwarts.library.service.LibroService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class LibroServiceImpl implements LibroService {

    private final LibroRepository libroRepository;
    private final CategoriaRepository categoriaRepository;
    private final AutorRepository autorRepository;
    private final LibroMapper libroMapper;

    public LibroServiceImpl(
            LibroRepository libroRepository,
            CategoriaRepository categoriaRepository,
            AutorRepository autorRepository,
            LibroMapper libroMapper
    ) {
        this.libroRepository = libroRepository;
        this.categoriaRepository = categoriaRepository;
        this.autorRepository = autorRepository;
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

        Set<Autor> autores = buscarAutoresActivos(request.autorIds());

        Libro libro = libroMapper.toEntity(request, categoria, autores);
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

        Set<Autor> autores = buscarAutoresActivos(request.autorIds());

        libroMapper.updateEntity(
                libro,
                request,
                categoria,
                autores
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
        libro.getAutores().forEach(this::validarAutorActivo);

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
                                "No existe una categoria con id: " + id
                        )
                );
    }

    private Set<Autor> buscarAutoresActivos(List<Long> autorIds) {

        if (autorIds == null || autorIds.isEmpty()) {
            throw new BusinessException("Debe seleccionar al menos un autor");
        }

        Set<Long> idsUnicos = new LinkedHashSet<>(autorIds);

        if (idsUnicos.size() != autorIds.size()) {
            throw new BusinessException("No se puede asociar el mismo autor mas de una vez al libro");
        }

        List<Autor> autores = autorRepository.findAllById(idsUnicos);

        if (autores.size() != idsUnicos.size()) {
            throw new ResourceNotFoundException("Uno o mas autores no existen");
        }

        autores.forEach(this::validarAutorActivo);

        return new LinkedHashSet<>(autores);
    }

    private void validarCategoriaActiva(Categoria categoria) {

        if (!Boolean.TRUE.equals(categoria.getActiva())) {
            throw new BusinessException(
                    "No se puede asociar un libro a una categoria inactiva"
            );
        }
    }

    private void validarAutorActivo(Autor autor) {

        if (!Boolean.TRUE.equals(autor.getActivo())) {
            throw new BusinessException(
                    "No se puede asociar un libro a un autor inactivo"
            );
        }
    }
}