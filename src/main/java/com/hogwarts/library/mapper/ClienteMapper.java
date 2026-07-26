package com.hogwarts.library.mapper;

import com.hogwarts.library.domain.Cliente;
import com.hogwarts.library.dto.cliente.ClienteRequest;
import com.hogwarts.library.dto.cliente.ClienteResponse;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public Cliente toEntity(ClienteRequest request) {

        Cliente cliente = new Cliente();

        cliente.setNombre(normalizar(request.nombre()));
        cliente.setApellido(normalizar(request.apellido()));
        cliente.setDni(normalizar(request.dni()));
        cliente.setEmail(normalizar(request.email()));
        cliente.setTelefono(normalizarNullable(request.telefono()));

        return cliente;
    }

    public ClienteResponse toResponse(Cliente cliente) {

        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getDni(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getFechaRegistro(),
                cliente.getActivo()
        );
    }

    public void updateEntity(
            Cliente cliente,
            ClienteRequest request
    ) {

        cliente.setNombre(normalizar(request.nombre()));
        cliente.setApellido(normalizar(request.apellido()));
        cliente.setDni(normalizar(request.dni()));
        cliente.setEmail(normalizar(request.email()));
        cliente.setTelefono(normalizarNullable(request.telefono()));
    }

    private String normalizar(String valor) {
        return valor.trim();
    }

    private String normalizarNullable(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}