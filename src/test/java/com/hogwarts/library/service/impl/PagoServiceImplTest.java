package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Alquiler;
import com.hogwarts.library.domain.Pago;
import com.hogwarts.library.domain.enums.EstadoAlquiler;
import com.hogwarts.library.domain.enums.MetodoPago;
import com.hogwarts.library.dto.pago.PagoRequest;
import com.hogwarts.library.dto.pago.PagoResponse;
import com.hogwarts.library.exception.BusinessException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.PagoMapper;
import com.hogwarts.library.repository.AlquilerRepository;
import com.hogwarts.library.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private AlquilerRepository alquilerRepository;

    @Mock
    private PagoMapper pagoMapper;

    private PagoServiceImpl pagoService;

    private Alquiler alquiler;

    @BeforeEach
    void setUp() {

        pagoService = new PagoServiceImpl(
                pagoRepository,
                alquilerRepository,
                pagoMapper
        );

        alquiler = new Alquiler();
        alquiler.setId(1L);
        alquiler.setEstado(EstadoAlquiler.ACTIVO);
        alquiler.setTotal(new BigDecimal("30000.00"));
    }

    @Test
    void registrar_deberiaRegistrarPagoCuandoEsValido() {

        // Arrange
        PagoRequest request = new PagoRequest(
                1L,
                new BigDecimal("10000.00"),
                MetodoPago.EFECTIVO,
                "Primer pago"
        );

        when(alquilerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(alquiler));

        when(pagoRepository.calcularTotalPagado(1L))
                .thenReturn(new BigDecimal("5000.00"));

        Pago pago = new Pago();
        pago.setAlquiler(alquiler);
        pago.setMonto(new BigDecimal("10000.00"));
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setObservaciones("Primer pago");

        when(pagoMapper.toEntity(request, alquiler))
                .thenReturn(pago);

        when(pagoRepository.save(any(Pago.class)))
                .thenAnswer(invocation -> {

                    Pago pagoGuardado = invocation.getArgument(0);
                    pagoGuardado.setId(10L);

                    return pagoGuardado;
                });

        PagoResponse responseEsperada = new PagoResponse(
                10L,
                1L,
                LocalDateTime.now(),
                new BigDecimal("10000.00"),
                MetodoPago.EFECTIVO,
                "Primer pago",
                new BigDecimal("30000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("15000.00")
        );

        when(
                pagoMapper.toResponse(
                        any(Pago.class),
                        eq(new BigDecimal("15000.00"))
                )
        ).thenReturn(responseEsperada);

        // Act
        PagoResponse resultado =
                pagoService.registrar(request);

        // Assert
        assertNotNull(resultado);

        assertEquals(
                10L,
                resultado.id()
        );

        assertEquals(
                new BigDecimal("10000.00"),
                resultado.monto()
        );

        assertEquals(
                new BigDecimal("15000.00"),
                resultado.totalPagado()
        );

        assertEquals(
                new BigDecimal("15000.00"),
                resultado.saldoPendiente()
        );

        verify(alquilerRepository)
                .findByIdForUpdate(1L);

        verify(pagoRepository)
                .calcularTotalPagado(1L);

        verify(pagoRepository)
                .save(any(Pago.class));

        verify(pagoMapper)
                .toResponse(
                        any(Pago.class),
                        eq(new BigDecimal("15000.00"))
                );
    }
    @Test
    void registrar_deberiaRechazarPagoCuandoSuperaSaldoPendiente() {

        // Arrange
        PagoRequest request = new PagoRequest(
                1L,
                new BigDecimal("16000.00"),
                MetodoPago.EFECTIVO,
                "Pago excesivo"
        );

        when(alquilerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(alquiler));

        // Total alquiler = 30000
        // Ya pagado = 15000
        // Saldo = 15000
        when(pagoRepository.calcularTotalPagado(1L))
                .thenReturn(new BigDecimal("15000.00"));

        // Act + Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pagoService.registrar(request)
        );

        assertEquals(
                "El monto del pago supera el saldo pendiente de 15000.00",
                exception.getMessage()
        );

        verify(alquilerRepository)
                .findByIdForUpdate(1L);

        verify(pagoRepository)
                .calcularTotalPagado(1L);

        verify(pagoRepository, never())
                .save(any(Pago.class));

        verifyNoInteractions(pagoMapper);
    }
    @Test
    void registrar_deberiaRechazarPagoCuandoAlquilerYaEstaTotalmentePagado() {

        // Arrange
        PagoRequest request = new PagoRequest(
                1L,
                new BigDecimal("1000.00"),
                MetodoPago.EFECTIVO,
                null
        );

        when(alquilerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(alquiler));

        // Total alquiler = 30000
        // Ya pagado = 30000
        // Saldo = 0
        when(pagoRepository.calcularTotalPagado(1L))
                .thenReturn(new BigDecimal("30000.00"));

        // Act + Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pagoService.registrar(request)
        );

        assertEquals(
                "El alquiler ya se encuentra totalmente pagado",
                exception.getMessage()
        );

        verify(pagoRepository, never())
                .save(any(Pago.class));

        verifyNoInteractions(pagoMapper);
    }
    @Test
    void registrar_deberiaRechazarPagoCuandoAlquilerEstaCancelado() {

        // Arrange
        alquiler.setEstado(EstadoAlquiler.CANCELADO);

        PagoRequest request = new PagoRequest(
                1L,
                new BigDecimal("1000.00"),
                MetodoPago.TRANSFERENCIA,
                null
        );

        when(alquilerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(alquiler));

        // Act + Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pagoService.registrar(request)
        );

        assertEquals(
                "No se pueden registrar pagos sobre un alquiler cancelado",
                exception.getMessage()
        );

        verify(alquilerRepository)
                .findByIdForUpdate(1L);

        // Ni siquiera debería calcular saldo.
        verify(pagoRepository, never())
                .calcularTotalPagado(anyLong());

        verify(pagoRepository, never())
                .save(any(Pago.class));

        verifyNoInteractions(pagoMapper);
    }
    @Test
    void registrar_deberiaLanzarExcepcionCuandoAlquilerNoExiste() {

        // Arrange
        PagoRequest request = new PagoRequest(
                999L,
                new BigDecimal("1000.00"),
                MetodoPago.EFECTIVO,
                null
        );

        when(alquilerRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> pagoService.registrar(request)
        );

        assertEquals(
                "No existe un alquiler con id: 999",
                exception.getMessage()
        );

        verify(alquilerRepository)
                .findByIdForUpdate(999L);

        verifyNoInteractions(pagoRepository);
        verifyNoInteractions(pagoMapper);
    }
}