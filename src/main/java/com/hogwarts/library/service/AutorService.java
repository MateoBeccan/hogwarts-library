package com.hogwarts.library.service;

import com.hogwarts.library.dto.autor.AutorRequest;
import com.hogwarts.library.dto.autor.AutorResponse;

import java.util.List;

public interface AutorService {

    AutorResponse crear(AutorRequest request);

    AutorResponse buscarPorId(Long id);

    List<AutorResponse> listar();

    AutorResponse actualizar(Long id, AutorRequest request);

    void desactivar(Long id);
}