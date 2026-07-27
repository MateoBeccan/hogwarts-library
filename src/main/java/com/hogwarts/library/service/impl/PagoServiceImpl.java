package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Alquiler;
import com.hogwarts.library.domain.Pago;
import com.hogwarts.library.domain.enums.EstadoAlquiler;
import com.hogwarts.library.dto.pago.PagoRequest;
import com.hogwarts.library.dto.pago.PagoResponse;
import com.hogwarts.library.exception.BusinessException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.PagoMapper;
import com.hogwarts.library.repository.AlquilerRepository;
import com.hogwarts.library.repository.PagoRepository;
import com.hogwarts.library.service.PagoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final AlquilerRepository alquilerRepository;
    private final PagoMapper pagoMapper;

    public PagoServiceImpl(
            PagoRepository pagoRepository,
            AlquilerRepository alquilerRepository,
            PagoMapper pagoMapper
    ) {
        this.pagoRepository = pagoRepository;
        this.alquilerRepository = alquilerRepository;
        this.pagoMapper = pagoMapper;
    }

    @Override
    public PagoResponse registrar(PagoRequest request) {

        // Se bloquea el alquiler durante toda la operación
        // para evitar sobrepagos concurrentes.
        Alquiler alquiler = buscarAlquilerPorIdConLock(
                request.alquilerId()
        );

        validarAlquilerParaPago(alquiler);

        BigDecimal totalPagadoActual =
                pagoRepository.calcularTotalPagado(
                        alquiler.getId()
                );

        BigDecimal saldoPendiente =
                alquiler.getTotal()
                        .subtract(totalPagadoActual);

        validarMontoContraSaldo(
                request.monto(),
                saldoPendiente
        );

        Pago pago = pagoMapper.toEntity(
                request,
                alquiler
        );

        pago.setFecha(LocalDateTime.now());

        Pago guardado = pagoRepository.save(pago);

        BigDecimal nuevoTotalPagado =
                totalPagadoActual.add(guardado.getMonto());

        return pagoMapper.toResponse(
                guardado,
                nuevoTotalPagado
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse buscarPorId(Long id) {

        Pago pago = buscarEntidadPorId(id);

        BigDecimal totalPagado =
                pagoRepository.calcularTotalPagado(
                        pago.getAlquiler().getId()
                );

        return pagoMapper.toResponse(
                pago,
                totalPagado
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listarPorAlquiler(Long alquilerId) {

        buscarAlquilerPorId(alquilerId);

        BigDecimal totalPagado =
                pagoRepository.calcularTotalPagado(alquilerId);

        return pagoRepository
                .findByAlquilerIdOrderByFechaAsc(alquilerId)
                .stream()
                .map(pago ->
                        pagoMapper.toResponse(
                                pago,
                                totalPagado
                        )
                )
                .toList();
    }

    private Pago buscarEntidadPorId(Long id) {

        return pagoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un pago con id: " + id
                        )
                );
    }

    private Alquiler buscarAlquilerPorId(Long id) {

        return alquilerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un alquiler con id: " + id
                        )
                );
    }

    private Alquiler buscarAlquilerPorIdConLock(Long id) {

        return alquilerRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un alquiler con id: " + id
                        )
                );
    }

    private void validarAlquilerParaPago(Alquiler alquiler) {

        if (alquiler.getEstado() == EstadoAlquiler.CANCELADO) {
            throw new BusinessException(
                    "No se pueden registrar pagos sobre un alquiler cancelado"
            );
        }
    }

    private void validarMontoContraSaldo(
            BigDecimal monto,
            BigDecimal saldoPendiente
    ) {

        if (saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "El alquiler ya se encuentra totalmente pagado"
            );
        }

        if (monto.compareTo(saldoPendiente) > 0) {
            throw new BusinessException(
                    "El monto del pago supera el saldo pendiente de "
                            + saldoPendiente
            );
        }
    }
}