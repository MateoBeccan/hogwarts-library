package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Cliente;
import com.hogwarts.library.dto.cliente.ClienteRequest;
import com.hogwarts.library.dto.cliente.ClienteResponse;
import com.hogwarts.library.exception.DuplicateResourceException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.ClienteMapper;
import com.hogwarts.library.repository.ClienteRepository;
import com.hogwarts.library.service.ClienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteServiceImpl(
            ClienteRepository clienteRepository,
            ClienteMapper clienteMapper
    ) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    @Override
    public ClienteResponse crear(ClienteRequest request) {

        String dni = request.dni().trim();

        if (clienteRepository.existsByDni(dni)) {
            throw new DuplicateResourceException(
                    "Ya existe un cliente con DNI: " + dni
            );
        }

        Cliente cliente = clienteMapper.toEntity(request);

        cliente.setFechaRegistro(LocalDate.now());
        cliente.setActivo(true);

        Cliente guardado = clienteRepository.save(cliente);

        return clienteMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {

        Cliente cliente = buscarEntidadPorId(id);

        return clienteMapper.toResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {

        return clienteRepository.findAll()
                .stream()
                .map(clienteMapper::toResponse)
                .toList();
    }

    @Override
    public ClienteResponse actualizar(
            Long id,
            ClienteRequest request
    ) {

        Cliente cliente = buscarEntidadPorId(id);

        String dni = request.dni().trim();

        if (clienteRepository.existsByDniAndIdNot(dni, id)) {
            throw new DuplicateResourceException(
                    "Ya existe otro cliente con DNI: " + dni
            );
        }

        clienteMapper.updateEntity(cliente, request);

        Cliente actualizado = clienteRepository.save(cliente);

        return clienteMapper.toResponse(actualizado);
    }

    @Override
    public void desactivar(Long id) {

        Cliente cliente = buscarEntidadPorId(id);

        cliente.setActivo(false);

        clienteRepository.save(cliente);
    }

    @Override
    public ClienteResponse reactivar(Long id) {

        Cliente cliente = buscarEntidadPorId(id);

        cliente.setActivo(true);

        Cliente actualizado = clienteRepository.save(cliente);

        return clienteMapper.toResponse(actualizado);
    }

    private Cliente buscarEntidadPorId(Long id) {

        return clienteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un cliente con id: " + id
                        )
                );
    }
}