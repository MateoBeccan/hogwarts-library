package com.hogwarts.library.service;

import com.hogwarts.library.dto.pago.PagoRequest;
import com.hogwarts.library.dto.pago.PagoResponse;

import java.util.List;

public interface PagoService {

    PagoResponse registrar(PagoRequest request);

    PagoResponse buscarPorId(Long id);

    List<PagoResponse> listarPorAlquiler(Long alquilerId);
}