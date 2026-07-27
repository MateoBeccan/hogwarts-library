package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlquilerServiceImplTest {

    @Mock
    private AlquilerRepository alquilerRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private EjemplarRepository ejemplarRepository;

    @Mock
    private AlquilerMapper alquilerMapper;

    private AlquilerServiceImpl alquilerService;

    private Cliente cliente;
    private Ejemplar ejemplar1;
    private Ejemplar ejemplar2;

    @BeforeEach
    void setUp() {

        alquilerService = new AlquilerServiceImpl(
                alquilerRepository,
                clienteRepository,
                ejemplarRepository,
                alquilerMapper
        );

        ReflectionTestUtils.setField(
                alquilerService,
                "maxEjemplares",
                3
        );

        ReflectionTestUtils.setField(
                alquilerService,
                "maxDias",
                14
        );

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Harry");
        cliente.setApellido("Potter");
        cliente.setActivo(true);

        Libro libro = new Libro();
        libro.setId(1L);
        libro.setTitulo("Harry Potter y la piedra filosofal");
        libro.setActivo(true);

        ejemplar1 = new Ejemplar();
        ejemplar1.setId(1L);
        ejemplar1.setCodigo("HOG-000001");
        ejemplar1.setEstado(EstadoEjemplar.DISPONIBLE);
        ejemplar1.setPrecioDia(new BigDecimal("1500.00"));
        ejemplar1.setLibro(libro);

        ejemplar2 = new Ejemplar();
        ejemplar2.setId(2L);
        ejemplar2.setCodigo("HOG-000002");
        ejemplar2.setEstado(EstadoEjemplar.DISPONIBLE);
        ejemplar2.setPrecioDia(new BigDecimal("1500.00"));
        ejemplar2.setLibro(libro);
    }

    @Test
    void crear_deberiaCrearAlquilerCuandoDatosSonValidos() {

        LocalDate vencimiento =
                LocalDate.now().plusDays(10);

        AlquilerRequest request = new AlquilerRequest(
                1L,
                vencimiento,
                List.of(1L, 2L),
                "Alquiler de prueba"
        );

        when(clienteRepository.findById(1L))
                .thenReturn(Optional.of(cliente));

        when(ejemplarRepository.findAllByIdForUpdate(
                List.of(1L, 2L)
        ))
                .thenReturn(List.of(ejemplar1, ejemplar2));

        when(alquilerRepository.save(any(Alquiler.class)))
                .thenAnswer(invocation -> {

                    Alquiler alquiler =
                            invocation.getArgument(0);

                    alquiler.setId(10L);

                    return alquiler;
                });

        AlquilerResponse response = mock(
                AlquilerResponse.class
        );

        when(alquilerMapper.toResponse(any(Alquiler.class)))
                .thenReturn(response);

        AlquilerResponse resultado =
                alquilerService.crear(request);

        assertNotNull(resultado);

        verify(clienteRepository)
                .findById(1L);

        verify(ejemplarRepository)
                .findAllByIdForUpdate(
                        List.of(1L, 2L)
                );

        verify(alquilerRepository)
                .save(any(Alquiler.class));

        assertEquals(
                EstadoEjemplar.ALQUILADO,
                ejemplar1.getEstado()
        );

        assertEquals(
                EstadoEjemplar.ALQUILADO,
                ejemplar2.getEstado()
        );
    }
    @Test
    void crear_deberiaRechazarAlquilerCuandoClienteEstaInactivo() {

        cliente.setActivo(false);

        AlquilerRequest request = new AlquilerRequest(
                1L,
                LocalDate.now().plusDays(7),
                List.of(1L),
                null
        );

        when(clienteRepository.findById(1L))
                .thenReturn(Optional.of(cliente));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> alquilerService.crear(request)
        );

        assertEquals(
                "El cliente está inactivo y no puede realizar alquileres",
                exception.getMessage()
        );

        verify(ejemplarRepository, never())
                .findAllByIdForUpdate(anyList());

        verify(alquilerRepository, never())
                .save(any(Alquiler.class));
    }
    @Test
    void crear_deberiaRechazarAlquilerCuandoEjemplarNoEstaDisponible() {

        ejemplar1.setEstado(EstadoEjemplar.ALQUILADO);

        AlquilerRequest request = new AlquilerRequest(
                1L,
                LocalDate.now().plusDays(7),
                List.of(1L),
                null
        );

        when(clienteRepository.findById(1L))
                .thenReturn(Optional.of(cliente));

        when(ejemplarRepository.findAllByIdForUpdate(List.of(1L)))
                .thenReturn(List.of(ejemplar1));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> alquilerService.crear(request)
        );

        assertEquals(
                "El ejemplar HOG-000001 no está disponible",
                exception.getMessage()
        );

        verify(alquilerRepository, never())
                .save(any(Alquiler.class));
    }
    @Test
    void crear_deberiaRechazarAlquilerCuandoHayIdsDuplicados() {

        AlquilerRequest request = new AlquilerRequest(
                1L,
                LocalDate.now().plusDays(7),
                List.of(1L, 1L),
                null
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> alquilerService.crear(request)
        );

        assertEquals(
                "No se puede incluir el mismo ejemplar más de una vez",
                exception.getMessage()
        );

        verifyNoInteractions(clienteRepository);
        verifyNoInteractions(ejemplarRepository);
        verifyNoInteractions(alquilerRepository);
    }
    @Test
    void crear_deberiaRechazarAlquilerCuandoSuperaCantidadMaximaDeEjemplares() {

        AlquilerRequest request = new AlquilerRequest(
                1L,
                LocalDate.now().plusDays(7),
                List.of(1L, 2L, 3L, 4L),
                null
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> alquilerService.crear(request)
        );

        assertEquals(
                "Un alquiler no puede contener más de 3 ejemplares",
                exception.getMessage()
        );

        verifyNoInteractions(clienteRepository);
        verifyNoInteractions(ejemplarRepository);
        verifyNoInteractions(alquilerRepository);
    }
    @Test
    void crear_deberiaRechazarAlquilerCuandoSuperaMaximoDeDias() {

        AlquilerRequest request = new AlquilerRequest(
                1L,
                LocalDate.now().plusDays(15),
                List.of(1L),
                null
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> alquilerService.crear(request)
        );

        assertEquals(
                "El alquiler no puede superar los 14 días",
                exception.getMessage()
        );

        verifyNoInteractions(clienteRepository);
        verifyNoInteractions(ejemplarRepository);
        verifyNoInteractions(alquilerRepository);
    }
    @Test
    void finalizar_deberiaLanzarExcepcionCuandoAlquilerNoExiste() {

        when(alquilerRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> alquilerService.finalizar(999L)
        );

        assertEquals(
                "No existe un alquiler con id: 999",
                exception.getMessage()
        );

        verify(alquilerRepository, never())
                .save(any(Alquiler.class));
    }
    @Test
    void finalizar_deberiaRechazarAlquilerCuandoNoEstaActivo() {

        Alquiler alquiler = new Alquiler();
        alquiler.setId(1L);
        alquiler.setEstado(EstadoAlquiler.FINALIZADO);

        when(alquilerRepository.findById(1L))
                .thenReturn(Optional.of(alquiler));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> alquilerService.finalizar(1L)
        );

        assertEquals(
                "Solo se puede finalizar un alquiler activo",
                exception.getMessage()
        );

        verify(alquilerRepository, never())
                .save(any(Alquiler.class));
    }

}