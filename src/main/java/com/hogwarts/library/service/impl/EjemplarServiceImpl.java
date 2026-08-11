package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Ejemplar;
import com.hogwarts.library.domain.Libro;
import com.hogwarts.library.domain.enums.EstadoEjemplar;
import com.hogwarts.library.dto.ejemplar.EjemplarRequest;
import com.hogwarts.library.dto.ejemplar.EjemplarResponse;
import com.hogwarts.library.exception.BusinessException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.EjemplarMapper;
import com.hogwarts.library.repository.EjemplarRepository;
import com.hogwarts.library.repository.LibroRepository;
import com.hogwarts.library.service.EjemplarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class EjemplarServiceImpl implements EjemplarService {

    private final EjemplarRepository ejemplarRepository;
    private final LibroRepository libroRepository;
    private final EjemplarMapper ejemplarMapper;
    private final EjemplarCodigoGenerator ejemplarCodigoGenerator;

    public EjemplarServiceImpl(
            EjemplarRepository ejemplarRepository,
            LibroRepository libroRepository,
            EjemplarMapper ejemplarMapper,
            EjemplarCodigoGenerator ejemplarCodigoGenerator
    ) {
        this.ejemplarRepository = ejemplarRepository;
        this.libroRepository = libroRepository;
        this.ejemplarMapper = ejemplarMapper;
        this.ejemplarCodigoGenerator = ejemplarCodigoGenerator;
    }

    @Override
    public EjemplarResponse crear(EjemplarRequest request) {

        Libro libro = buscarLibroPorId(request.libroId());

        validarLibroActivo(libro);

        Ejemplar ejemplar = ejemplarMapper.toEntity(
                request,
                libro
        );

        ejemplar.setCodigo(ejemplarCodigoGenerator.generarSiguienteCodigo());
        ejemplar.setEstado(EstadoEjemplar.DISPONIBLE);
        ejemplar.setFechaAlta(LocalDate.now());

        Ejemplar guardado = ejemplarRepository.save(ejemplar);

        return ejemplarMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public EjemplarResponse buscarPorId(Long id) {

        Ejemplar ejemplar = buscarEntidadPorId(id);

        return ejemplarMapper.toResponse(ejemplar);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EjemplarResponse> listar() {

        return ejemplarRepository.findAll()
                .stream()
                .map(ejemplarMapper::toResponse)
                .toList();
    }

    @Override
    public EjemplarResponse actualizar(
            Long id,
            EjemplarRequest request
    ) {

        Ejemplar ejemplar = buscarEntidadPorId(id);

        validarEjemplarModificable(ejemplar);

        Libro libro = buscarLibroPorId(request.libroId());

        validarLibroActivo(libro);

        ejemplarMapper.updateEntity(
                ejemplar,
                request,
                libro
        );

        Ejemplar actualizado = ejemplarRepository.save(ejemplar);

        return ejemplarMapper.toResponse(actualizado);
    }

    @Override
    public EjemplarResponse marcarFueraServicio(Long id) {

        Ejemplar ejemplar = buscarEntidadPorId(id);

        if (ejemplar.getEstado() == EstadoEjemplar.ALQUILADO) {
            throw new BusinessException(
                    "No se puede poner fuera de servicio un ejemplar que estÃƒÂ¡ alquilado"
            );
        }

        ejemplar.setEstado(EstadoEjemplar.FUERA_SERVICIO);

        Ejemplar actualizado = ejemplarRepository.save(ejemplar);

        return ejemplarMapper.toResponse(actualizado);
    }

    @Override
    public EjemplarResponse habilitar(Long id) {

        Ejemplar ejemplar = buscarEntidadPorId(id);

        if (ejemplar.getEstado() != EstadoEjemplar.FUERA_SERVICIO) {
            throw new BusinessException(
                    "Solo se puede habilitar un ejemplar que estÃƒÂ¡ fuera de servicio"
            );
        }

        validarLibroActivo(ejemplar.getLibro());

        ejemplar.setEstado(EstadoEjemplar.DISPONIBLE);

        Ejemplar actualizado = ejemplarRepository.save(ejemplar);

        return ejemplarMapper.toResponse(actualizado);
    }

    private Ejemplar buscarEntidadPorId(Long id) {

        return ejemplarRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un ejemplar con id: " + id
                        )
                );
    }

    private Libro buscarLibroPorId(Long id) {

        return libroRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un libro con id: " + id
                        )
                );
    }

    private void validarLibroActivo(Libro libro) {

        if (!Boolean.TRUE.equals(libro.getActivo())) {
            throw new BusinessException(
                    "No se puede asociar un ejemplar a un libro inactivo"
            );
        }
    }

    private void validarEjemplarModificable(Ejemplar ejemplar) {

        if (ejemplar.getEstado() == EstadoEjemplar.ALQUILADO) {
            throw new BusinessException(
                    "No se puede modificar un ejemplar mientras estÃƒÂ¡ alquilado"
            );
        }
    }
}
