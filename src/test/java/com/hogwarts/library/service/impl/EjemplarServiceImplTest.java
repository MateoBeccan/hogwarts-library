package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Ejemplar;
import com.hogwarts.library.domain.Libro;
import com.hogwarts.library.domain.enums.EstadoEjemplar;
import com.hogwarts.library.dto.ejemplar.EjemplarRequest;
import com.hogwarts.library.dto.ejemplar.EjemplarResponse;
import com.hogwarts.library.mapper.EjemplarMapper;
import com.hogwarts.library.repository.EjemplarRepository;
import com.hogwarts.library.repository.LibroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EjemplarServiceImplTest {

    @Mock
    private EjemplarRepository ejemplarRepository;

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private EjemplarCodigoGenerator ejemplarCodigoGenerator;

    private EjemplarServiceImpl ejemplarService;

    private Libro libro;

    @BeforeEach
    void setUp() {

        ejemplarService = new EjemplarServiceImpl(
                ejemplarRepository,
                libroRepository,
                new EjemplarMapper(),
                ejemplarCodigoGenerator
        );

        libro = new Libro();
        libro.setId(7L);
        libro.setTitulo("Defensa Contra las Artes Oscuras");
        libro.setIsbn("978-0000000007");
        libro.setActivo(true);
    }

    @Test
    void crear_deberiaGenerarCodigoAutomatico() {

        EjemplarRequest request = new EjemplarRequest(
                new BigDecimal("1500.00"),
                "Estante A",
                7L
        );

        when(libroRepository.findById(7L))
                .thenReturn(Optional.of(libro));
        when(ejemplarCodigoGenerator.generarSiguienteCodigo())
                .thenReturn("HOG-000004");
        when(ejemplarRepository.save(any(Ejemplar.class)))
                .thenAnswer(invocation -> {
                    Ejemplar ejemplar = invocation.getArgument(0);
                    ejemplar.setId(10L);
                    return ejemplar;
                });

        EjemplarResponse response = ejemplarService.crear(request);

        assertEquals("HOG-000004", response.codigo());
        assertEquals(EstadoEjemplar.DISPONIBLE, response.estado());
        verify(ejemplarCodigoGenerator).generarSiguienteCodigo();
    }

    @Test
    void crear_noDebeDependerDeCodigoEnviadoPorCliente() {

        EjemplarRequest request = new EjemplarRequest(
                new BigDecimal("2000.00"),
                null,
                7L
        );

        when(libroRepository.findById(7L))
                .thenReturn(Optional.of(libro));
        when(ejemplarCodigoGenerator.generarSiguienteCodigo())
                .thenReturn("HOG-000004");
        when(ejemplarRepository.save(any(Ejemplar.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EjemplarResponse response = ejemplarService.crear(request);

        assertEquals("HOG-000004", response.codigo());
        verify(ejemplarRepository, never()).existsByCodigo(any());
    }

    @Test
    void actualizar_deberiaConservarCodigoOriginal() {

        Ejemplar ejemplar = new Ejemplar();
        ejemplar.setId(20L);
        ejemplar.setCodigo("HOG-000009");
        ejemplar.setEstado(EstadoEjemplar.DISPONIBLE);
        ejemplar.setPrecioDia(new BigDecimal("1000.00"));
        ejemplar.setLibro(libro);

        EjemplarRequest request = new EjemplarRequest(
                new BigDecimal("1800.00"),
                "Actualizado",
                7L
        );

        when(ejemplarRepository.findById(20L))
                .thenReturn(Optional.of(ejemplar));
        when(libroRepository.findById(7L))
                .thenReturn(Optional.of(libro));
        when(ejemplarRepository.save(ejemplar))
                .thenReturn(ejemplar);

        EjemplarResponse response = ejemplarService.actualizar(20L, request);

        assertEquals("HOG-000009", response.codigo());
        assertEquals(new BigDecimal("1800.00"), response.precioDia());
        verify(ejemplarCodigoGenerator, never()).generarSiguienteCodigo();
        verify(ejemplarRepository, never()).existsByCodigoAndIdNot(any(), any());
    }
}
