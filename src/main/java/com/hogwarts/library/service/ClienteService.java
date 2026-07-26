package com.hogwarts.library.service;

import com.hogwarts.library.dto.cliente.ClienteRequest;
import com.hogwarts.library.dto.cliente.ClienteResponse;

import java.util.List;

public interface ClienteService {

    ClienteResponse crear(ClienteRequest request);

    ClienteResponse buscarPorId(Long id);

    List<ClienteResponse> listar();

    ClienteResponse actualizar(Long id, ClienteRequest request);

    void desactivar(Long id);

    ClienteResponse reactivar(Long id);
}