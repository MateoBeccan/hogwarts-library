package com.hogwarts.library.service;

import com.hogwarts.library.dto.ejemplar.EjemplarRequest;
import com.hogwarts.library.dto.ejemplar.EjemplarResponse;

import java.util.List;

public interface EjemplarService {

    EjemplarResponse crear(EjemplarRequest request);

    EjemplarResponse buscarPorId(Long id);

    List<EjemplarResponse> listar();

    EjemplarResponse actualizar(Long id, EjemplarRequest request);

    EjemplarResponse marcarFueraServicio(Long id);

    EjemplarResponse habilitar(Long id);
}