package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Alquiler;
import com.hogwarts.library.domain.Cliente;
import com.hogwarts.library.domain.DetalleAlquiler;
import com.hogwarts.library.domain.Ejemplar;
import com.hogwarts.library.domain.enums.EstadoAlquiler;
import com.hogwarts.library.domain.enums.EstadoEjemplar;
import com.hogwarts.library.dto.alquiler.AlquilerRequest;
import com.hogwarts.library.dto.alquiler.AlquilerResponse;
import com.hogwarts.library.exception.BusinessException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.AlquilerMapper;
import com.hogwarts.library.repository.AlquilerRepository;
import com.hogwarts.library.repository.ClienteRepository;
import com.hogwarts.library.repository.EjemplarRepository;
import com.hogwarts.library.service.AlquilerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AlquilerServiceImpl implements AlquilerService {

    private final AlquilerRepository alquilerRepository;
    private final ClienteRepository clienteRepository;
    private final EjemplarRepository ejemplarRepository;
    private final AlquilerMapper alquilerMapper;

    @Value("${app.alquiler.max-ejemplares}")
    private int maxEjemplares;

    @Value("${app.alquiler.max-dias}")
    private int maxDias;

    public AlquilerServiceImpl(
            AlquilerRepository alquilerRepository,
            ClienteRepository clienteRepository,
            EjemplarRepository ejemplarRepository,
            AlquilerMapper alquilerMapper
    ) {
        this.alquilerRepository = alquilerRepository;
        this.clienteRepository = clienteRepository;
        this.ejemplarRepository = ejemplarRepository;
        this.alquilerMapper = alquilerMapper;
    }

    @Override
    public AlquilerResponse crear(AlquilerRequest request) {

        LocalDate fechaAlquiler = LocalDate.now();

        validarCantidadEjemplares(request.ejemplarIds());

        validarIdsDuplicados(request.ejemplarIds());

        int cantidadDias = calcularCantidadDias(
                fechaAlquiler,
                request.fechaVencimiento()
        );

        Cliente cliente = buscarClientePorId(request.clienteId());

        validarClienteActivo(cliente);

        List<Ejemplar> ejemplares =
                ejemplarRepository.findAllByIdForUpdate(
                        request.ejemplarIds()
                );

        validarQueExistanTodos(
                ejemplares,
                request.ejemplarIds()
        );

        validarEjemplaresDisponibles(ejemplares);

        validarLibrosActivos(ejemplares);

        Alquiler alquiler = new Alquiler();

        alquiler.setCliente(cliente);
        alquiler.setFechaAlquiler(fechaAlquiler);
        alquiler.setFechaVencimiento(request.fechaVencimiento());
        alquiler.setFechaDevolucion(null);
        alquiler.setEstado(EstadoAlquiler.ACTIVO);
        alquiler.setObservaciones(
                normalizarNullable(request.observaciones())
        );

        List<DetalleAlquiler> detalles = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        for (Ejemplar ejemplar : ejemplares) {

            BigDecimal precioDia = ejemplar.getPrecioDia();

            BigDecimal subtotal = precioDia.multiply(
                    BigDecimal.valueOf(cantidadDias)
            );

            DetalleAlquiler detalle = new DetalleAlquiler();

            detalle.setAlquiler(alquiler);
            detalle.setEjemplar(ejemplar);

            // Snapshot histórico.
            detalle.setPrecioDia(precioDia);
            detalle.setCantidadDias(cantidadDias);
            detalle.setSubtotal(subtotal);

            detalles.add(detalle);

            total = total.add(subtotal);

            ejemplar.setEstado(EstadoEjemplar.ALQUILADO);
        }

        alquiler.setDetalles(detalles);
        alquiler.setTotal(total);

        Alquiler guardado = alquilerRepository.save(alquiler);

        return alquilerMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public AlquilerResponse buscarPorId(Long id) {

        Alquiler alquiler = buscarEntidadPorId(id);

        return alquilerMapper.toResponse(alquiler);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlquilerResponse> listar() {

        return alquilerRepository.findAll()
                .stream()
                .map(alquilerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlquilerResponse> listarPorCliente(Long clienteId) {

        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException(
                    "No existe un cliente con id: " + clienteId
            );
        }

        return alquilerRepository.findByClienteId(clienteId)
                .stream()
                .map(alquilerMapper::toResponse)
                .toList();
    }

    @Override
    public AlquilerResponse finalizar(Long id) {

        Alquiler alquiler = buscarEntidadPorId(id);

        if (alquiler.getEstado() != EstadoAlquiler.ACTIVO) {
            throw new BusinessException(
                    "Solo se puede finalizar un alquiler activo"
            );
        }

        for (DetalleAlquiler detalle : alquiler.getDetalles()) {

            Ejemplar ejemplar = detalle.getEjemplar();

            if (ejemplar.getEstado() == EstadoEjemplar.ALQUILADO) {
                ejemplar.setEstado(EstadoEjemplar.DISPONIBLE);
            }
        }

        alquiler.setEstado(EstadoAlquiler.FINALIZADO);
        alquiler.setFechaDevolucion(LocalDate.now());

        Alquiler actualizado = alquilerRepository.save(alquiler);

        return alquilerMapper.toResponse(actualizado);
    }

    @Override
    public AlquilerResponse cancelar(Long id) {

        Alquiler alquiler = buscarEntidadPorId(id);

        if (alquiler.getEstado() != EstadoAlquiler.ACTIVO) {
            throw new BusinessException(
                    "Solo se puede cancelar un alquiler activo"
            );
        }

        for (DetalleAlquiler detalle : alquiler.getDetalles()) {

            Ejemplar ejemplar = detalle.getEjemplar();

            if (ejemplar.getEstado() == EstadoEjemplar.ALQUILADO) {
                ejemplar.setEstado(EstadoEjemplar.DISPONIBLE);
            }
        }

        alquiler.setEstado(EstadoAlquiler.CANCELADO);

        Alquiler actualizado = alquilerRepository.save(alquiler);

        return alquilerMapper.toResponse(actualizado);
    }

    private Alquiler buscarEntidadPorId(Long id) {

        return alquilerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un alquiler con id: " + id
                        )
                );
    }

    private Cliente buscarClientePorId(Long id) {

        return clienteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un cliente con id: " + id
                        )
                );
    }

    private void validarClienteActivo(Cliente cliente) {

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new BusinessException(
                    "El cliente está inactivo y no puede realizar alquileres"
            );
        }
    }

    private void validarCantidadEjemplares(List<Long> ids) {

        if (ids.size() > maxEjemplares) {
            throw new BusinessException(
                    "Un alquiler no puede contener más de "
                            + maxEjemplares
                            + " ejemplares"
            );
        }
    }

    private void validarIdsDuplicados(List<Long> ids) {

        Set<Long> idsUnicos = new HashSet<>(ids);

        if (idsUnicos.size() != ids.size()) {
            throw new BusinessException(
                    "No se puede incluir el mismo ejemplar más de una vez"
            );
        }
    }

    private int calcularCantidadDias(
            LocalDate fechaAlquiler,
            LocalDate fechaVencimiento
    ) {

        long dias = ChronoUnit.DAYS.between(
                fechaAlquiler,
                fechaVencimiento
        );

        if (dias <= 0) {
            throw new BusinessException(
                    "La fecha de vencimiento debe ser posterior a la fecha de alquiler"
            );
        }

        if (dias > maxDias) {
            throw new BusinessException(
                    "El alquiler no puede superar los "
                            + maxDias
                            + " días"
            );
        }

        return Math.toIntExact(dias);
    }

    private void validarQueExistanTodos(
            List<Ejemplar> ejemplares,
            List<Long> idsSolicitados
    ) {

        if (ejemplares.size() != idsSolicitados.size()) {
            throw new ResourceNotFoundException(
                    "Uno o más ejemplares no existen"
            );
        }
    }

    private void validarEjemplaresDisponibles(
            List<Ejemplar> ejemplares
    ) {

        for (Ejemplar ejemplar : ejemplares) {

            if (ejemplar.getEstado() != EstadoEjemplar.DISPONIBLE) {
                throw new BusinessException(
                        "El ejemplar "
                                + ejemplar.getCodigo()
                                + " no está disponible"
                );
            }
        }
    }

    private void validarLibrosActivos(
            List<Ejemplar> ejemplares
    ) {

        for (Ejemplar ejemplar : ejemplares) {

            if (!Boolean.TRUE.equals(
                    ejemplar.getLibro().getActivo()
            )) {
                throw new BusinessException(
                        "El libro asociado al ejemplar "
                                + ejemplar.getCodigo()
                                + " está inactivo"
                );
            }
        }
    }

    private String normalizarNullable(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}