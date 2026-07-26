package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Categoria;
import com.hogwarts.library.dto.categoria.CategoriaRequest;
import com.hogwarts.library.dto.categoria.CategoriaResponse;
import com.hogwarts.library.exception.DuplicateResourceException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.CategoriaMapper;
import com.hogwarts.library.repository.CategoriaRepository;
import com.hogwarts.library.service.CategoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    public CategoriaServiceImpl(
            CategoriaRepository categoriaRepository,
            CategoriaMapper categoriaMapper
    ) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaMapper = categoriaMapper;
    }

    @Override
    public CategoriaResponse crear(CategoriaRequest request) {

        String nombreNormalizado = request.nombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new DuplicateResourceException(
                    "Ya existe una categoría con el nombre: " + nombreNormalizado
            );
        }

        Categoria categoria = categoriaMapper.toEntity(request);

        categoria.setNombre(nombreNormalizado);
        categoria.setActiva(true);

        Categoria categoriaGuardada = categoriaRepository.save(categoria);

        return categoriaMapper.toResponse(categoriaGuardada);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {

        Categoria categoria = buscarEntidadPorId(id);

        return categoriaMapper.toResponse(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {

        return categoriaRepository.findAll()
                .stream()
                .map(categoriaMapper::toResponse)
                .toList();
    }

    @Override
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {

        Categoria categoria = buscarEntidadPorId(id);

        String nombreNormalizado = request.nombre().trim();

        boolean nombreEnUso =
                categoriaRepository.existsByNombreIgnoreCaseAndIdNot(
                        nombreNormalizado,
                        id
                );

        if (nombreEnUso) {
            throw new DuplicateResourceException(
                    "Ya existe otra categoría con el nombre: " + nombreNormalizado
            );
        }

        categoriaMapper.updateEntity(categoria, request);

        categoria.setNombre(nombreNormalizado);

        Categoria categoriaActualizada = categoriaRepository.save(categoria);

        return categoriaMapper.toResponse(categoriaActualizada);
    }

    @Override
    public void desactivar(Long id) {

        Categoria categoria = buscarEntidadPorId(id);

        categoria.setActiva(false);

        categoriaRepository.save(categoria);
    }

    private Categoria buscarEntidadPorId(Long id) {

        return categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe una categoría con id: " + id
                        )
                );
    }
}