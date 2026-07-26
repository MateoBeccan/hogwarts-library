package com.hogwarts.library.service;

import com.hogwarts.library.dto.libro.LibroRequest;
import com.hogwarts.library.dto.libro.LibroResponse;

import java.util.List;

public interface LibroService {

    LibroResponse crear(LibroRequest request);

    LibroResponse buscarPorId(Long id);

    List<LibroResponse> listar();

    LibroResponse actualizar(Long id, LibroRequest request);

    void desactivar(Long id);

    LibroResponse reactivar(Long id);
}