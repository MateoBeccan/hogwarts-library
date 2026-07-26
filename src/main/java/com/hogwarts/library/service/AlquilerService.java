package com.hogwarts.library.service;

import com.hogwarts.library.dto.alquiler.AlquilerRequest;
import com.hogwarts.library.dto.alquiler.AlquilerResponse;
import lombok.Value;

import java.util.List;

public interface AlquilerService {

    AlquilerResponse crear(AlquilerRequest request);

    AlquilerResponse buscarPorId(Long id);

    List<AlquilerResponse> listar();

    List<AlquilerResponse> listarPorCliente(Long clienteId);

    AlquilerResponse finalizar(Long id);

    AlquilerResponse cancelar(Long id);
}