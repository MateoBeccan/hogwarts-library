package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Autor;
import com.hogwarts.library.dto.autor.AutorRequest;
import com.hogwarts.library.dto.autor.AutorResponse;
import com.hogwarts.library.exception.DuplicateResourceException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.AutorMapper;
import com.hogwarts.library.repository.AutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorServiceImplTest {

    @Mock
    private AutorRepository autorRepository;

    private AutorServiceImpl autorService;

    @BeforeEach
    void setUp() {
        autorService = new AutorServiceImpl(
                autorRepository,
                new AutorMapper()
        );
    }

    @Test
    void crear_deberiaCrearAutorActivoConNombreNormalizado() {

        AutorRequest request = new AutorRequest("  J. K. Rowling  ");

        when(autorRepository.existsByNombreCompletoIgnoreCase("J. K. Rowling"))
                .thenReturn(false);
        when(autorRepository.save(any(Autor.class)))
                .thenAnswer(invocation -> {
                    Autor autor = invocation.getArgument(0);
                    autor.setId(1L);
                    return autor;
                });

        AutorResponse response = autorService.crear(request);

        assertEquals(1L, response.id());
        assertEquals("J. K. Rowling", response.nombreCompleto());
        assertTrue(response.activo());
    }

    @Test
    void crear_deberiaRechazarNombreDuplicadoIgnorandoMayusculas() {

        AutorRequest request = new AutorRequest("J. K. Rowling");

        when(autorRepository.existsByNombreCompletoIgnoreCase("J. K. Rowling"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> autorService.crear(request)
        );

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void listar_deberiaResponderAutoresExistentes() {

        Autor autor1 = autor(1L, "J. K. Rowling", true);
        Autor autor2 = autor(2L, "Mary Shelley", false);

        when(autorRepository.findAll())
                .thenReturn(List.of(autor1, autor2));

        List<AutorResponse> responses = autorService.listar();

        assertEquals(2, responses.size());
        assertEquals("J. K. Rowling", responses.get(0).nombreCompleto());
        assertFalse(responses.get(1).activo());
    }

    @Test
    void actualizar_deberiaRechazarNombreDuplicadoEnOtroAutor() {

        Autor autor = autor(1L, "Nombre anterior", true);
        AutorRequest request = new AutorRequest("Mary Shelley");

        when(autorRepository.findById(1L))
                .thenReturn(Optional.of(autor));
        when(autorRepository.existsByNombreCompletoIgnoreCaseAndIdNot("Mary Shelley", 1L))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> autorService.actualizar(1L, request)
        );

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void desactivar_deberiaMarcarAutorComoInactivo() {

        Autor autor = autor(1L, "Mary Shelley", true);

        when(autorRepository.findById(1L))
                .thenReturn(Optional.of(autor));

        autorService.desactivar(1L);

        assertFalse(autor.getActivo());
        verify(autorRepository).save(autor);
    }

    @Test
    void buscarPorId_deberiaLanzarExcepcionCuandoNoExiste() {

        when(autorRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> autorService.buscarPorId(99L)
        );
    }

    private Autor autor(Long id, String nombreCompleto, boolean activo) {
        Autor autor = new Autor();
        autor.setId(id);
        autor.setNombreCompleto(nombreCompleto);
        autor.setActivo(activo);
        return autor;
    }
}