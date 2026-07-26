package com.hogwarts.library.service;

import com.hogwarts.library.dto.categoria.CategoriaRequest;
import com.hogwarts.library.dto.categoria.CategoriaResponse;

import java.util.List;

public interface CategoriaService {

    CategoriaResponse crear(CategoriaRequest request);

    CategoriaResponse buscarPorId(Long id);

    List<CategoriaResponse> listar();

    CategoriaResponse actualizar(Long id, CategoriaRequest request);

    void desactivar(Long id);
}