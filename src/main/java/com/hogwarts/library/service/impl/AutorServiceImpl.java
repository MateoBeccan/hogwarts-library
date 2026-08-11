package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Autor;
import com.hogwarts.library.dto.autor.AutorRequest;
import com.hogwarts.library.dto.autor.AutorResponse;
import com.hogwarts.library.exception.DuplicateResourceException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.AutorMapper;
import com.hogwarts.library.repository.AutorRepository;
import com.hogwarts.library.service.AutorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AutorServiceImpl implements AutorService {

    private final AutorRepository autorRepository;
    private final AutorMapper autorMapper;

    public AutorServiceImpl(
            AutorRepository autorRepository,
            AutorMapper autorMapper
    ) {
        this.autorRepository = autorRepository;
        this.autorMapper = autorMapper;
    }

    @Override
    public AutorResponse crear(AutorRequest request) {

        String nombreNormalizado = request.nombreCompleto().trim();

        if (autorRepository.existsByNombreCompletoIgnoreCase(nombreNormalizado)) {
            throw new DuplicateResourceException(
                    "Ya existe un autor con el nombre: " + nombreNormalizado
            );
        }

        Autor autor = autorMapper.toEntity(request);
        autor.setNombreCompleto(nombreNormalizado);
        autor.setActivo(true);

        Autor guardado = autorRepository.save(autor);

        return autorMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public AutorResponse buscarPorId(Long id) {
        return autorMapper.toResponse(buscarEntidadPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutorResponse> listar() {
        return autorRepository.findAll()
                .stream()
                .map(autorMapper::toResponse)
                .toList();
    }

    @Override
    public AutorResponse actualizar(Long id, AutorRequest request) {

        Autor autor = buscarEntidadPorId(id);
        String nombreNormalizado = request.nombreCompleto().trim();

        if (autorRepository.existsByNombreCompletoIgnoreCaseAndIdNot(nombreNormalizado, id)) {
            throw new DuplicateResourceException(
                    "Ya existe otro autor con el nombre: " + nombreNormalizado
            );
        }

        autorMapper.updateEntity(autor, request);
        autor.setNombreCompleto(nombreNormalizado);

        Autor actualizado = autorRepository.save(autor);

        return autorMapper.toResponse(actualizado);
    }

    @Override
    public void desactivar(Long id) {
        Autor autor = buscarEntidadPorId(id);
        autor.setActivo(false);
        autorRepository.save(autor);
    }

    private Autor buscarEntidadPorId(Long id) {
        return autorRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un autor con id: " + id
                        )
                );
    }
}