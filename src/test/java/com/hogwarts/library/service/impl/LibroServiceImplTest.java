package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.Autor;
import com.hogwarts.library.domain.Categoria;
import com.hogwarts.library.domain.Libro;
import com.hogwarts.library.dto.libro.LibroRequest;
import com.hogwarts.library.dto.libro.LibroResponse;
import com.hogwarts.library.exception.BusinessException;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.mapper.LibroMapper;
import com.hogwarts.library.repository.AutorRepository;
import com.hogwarts.library.repository.CategoriaRepository;
import com.hogwarts.library.repository.LibroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
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
class LibroServiceImplTest {

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private AutorRepository autorRepository;

    private LibroServiceImpl libroService;
    private Categoria categoria;
    private Autor autor1;
    private Autor autor2;
    private Autor autor3;

    @BeforeEach
    void setUp() {
        libroService = new LibroServiceImpl(
                libroRepository,
                categoriaRepository,
                autorRepository,
                new LibroMapper()
        );

        categoria = new Categoria();
        categoria.setId(4L);
        categoria.setNombre("Fantasia");
        categoria.setActiva(true);

        autor1 = autor(1L, "J. K. Rowling", true);
        autor2 = autor(2L, "Mary Shelley", true);
        autor3 = autor(3L, "Autor inactivo", false);
    }

    @Test
    void crear_deberiaCrearLibroConUnAutor() {

        LibroRequest request = request(List.of(1L));

        when(libroRepository.existsByIsbn("978-0000000001"))
                .thenReturn(false);
        when(categoriaRepository.findById(4L))
                .thenReturn(Optional.of(categoria));
        when(autorRepository.findAllById(any()))
                .thenReturn(List.of(autor1));
        when(libroRepository.save(any(Libro.class)))
                .thenAnswer(invocation -> {
                    Libro libro = invocation.getArgument(0);
                    libro.setId(10L);
                    return libro;
                });

        LibroResponse response = libroService.crear(request);

        assertEquals(10L, response.id());
        assertEquals("Harry Potter y la piedra filosofal", response.titulo());
        assertEquals(1, response.autores().size());
        assertEquals("J. K. Rowling", response.autores().get(0).nombreCompleto());
        assertTrue(response.activo());
    }

    @Test
    void crear_deberiaCrearLibroConVariosAutores() {

        LibroRequest request = request(List.of(1L, 2L));

        when(libroRepository.existsByIsbn("978-0000000001"))
                .thenReturn(false);
        when(categoriaRepository.findById(4L))
                .thenReturn(Optional.of(categoria));
        when(autorRepository.findAllById(any()))
                .thenReturn(List.of(autor2, autor1));
        when(libroRepository.save(any(Libro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LibroResponse response = libroService.crear(request);

        assertEquals(2, response.autores().size());
        assertEquals("J. K. Rowling", response.autores().get(0).nombreCompleto());
        assertEquals("Mary Shelley", response.autores().get(1).nombreCompleto());
    }

    @Test
    void crear_deberiaRechazarLibroSinAutores() {

        LibroRequest request = request(List.of());

        when(libroRepository.existsByIsbn("978-0000000001"))
                .thenReturn(false);
        when(categoriaRepository.findById(4L))
                .thenReturn(Optional.of(categoria));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> libroService.crear(request)
        );

        assertEquals("Debe seleccionar al menos un autor", exception.getMessage());
        verify(autorRepository, never()).findAllById(any());
        verify(libroRepository, never()).save(any(Libro.class));
    }

    @Test
    void crear_deberiaRechazarAutorDuplicado() {

        LibroRequest request = request(List.of(1L, 1L));

        when(libroRepository.existsByIsbn("978-0000000001"))
                .thenReturn(false);
        when(categoriaRepository.findById(4L))
                .thenReturn(Optional.of(categoria));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> libroService.crear(request)
        );

        assertEquals("No se puede asociar el mismo autor mas de una vez al libro", exception.getMessage());
        verify(autorRepository, never()).findAllById(any());
        verify(libroRepository, never()).save(any(Libro.class));
    }

    @Test
    void crear_deberiaRechazarAutorInexistente() {

        LibroRequest request = request(List.of(1L, 99L));

        when(libroRepository.existsByIsbn("978-0000000001"))
                .thenReturn(false);
        when(categoriaRepository.findById(4L))
                .thenReturn(Optional.of(categoria));
        when(autorRepository.findAllById(any()))
                .thenReturn(List.of(autor1));

        assertThrows(
                ResourceNotFoundException.class,
                () -> libroService.crear(request)
        );

        verify(libroRepository, never()).save(any(Libro.class));
    }

    @Test
    void crear_deberiaRechazarAutorInactivo() {

        LibroRequest request = request(List.of(3L));

        when(libroRepository.existsByIsbn("978-0000000001"))
                .thenReturn(false);
        when(categoriaRepository.findById(4L))
                .thenReturn(Optional.of(categoria));
        when(autorRepository.findAllById(any()))
                .thenReturn(List.of(autor3));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> libroService.crear(request)
        );

        assertEquals("No se puede asociar un libro a un autor inactivo", exception.getMessage());
        verify(libroRepository, never()).save(any(Libro.class));
    }

    @Test
    void actualizar_deberiaModificarAsociacionesDeAutores() {

        Libro libro = libro(List.of(autor1));
        LibroRequest request = request(List.of(2L));

        when(libroRepository.findById(10L))
                .thenReturn(Optional.of(libro));
        when(libroRepository.existsByIsbnAndIdNot("978-0000000001", 10L))
                .thenReturn(false);
        when(categoriaRepository.findById(4L))
                .thenReturn(Optional.of(categoria));
        when(autorRepository.findAllById(any()))
                .thenReturn(List.of(autor2));
        when(libroRepository.save(libro))
                .thenReturn(libro);

        LibroResponse response = libroService.actualizar(10L, request);

        assertEquals(1, response.autores().size());
        assertEquals("Mary Shelley", response.autores().get(0).nombreCompleto());
        assertFalse(libro.getAutores().contains(autor1));
        assertTrue(libro.getAutores().contains(autor2));
    }

    @Test
    void reactivar_deberiaRechazarLibroConAutorInactivo() {

        Libro libro = libro(List.of(autor3));
        libro.setActivo(false);

        when(libroRepository.findById(10L))
                .thenReturn(Optional.of(libro));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> libroService.reactivar(10L)
        );

        assertEquals("No se puede asociar un libro a un autor inactivo", exception.getMessage());
        verify(libroRepository, never()).save(any(Libro.class));
    }

    private LibroRequest request(List<Long> autorIds) {
        return new LibroRequest(
                " 978-0000000001 ",
                " Harry Potter y la piedra filosofal ",
                autorIds,
                " Bloomsbury ",
                1997,
                " Primer curso en Hogwarts ",
                4L
        );
    }

    private Libro libro(List<Autor> autores) {
        Libro libro = new Libro();
        libro.setId(10L);
        libro.setIsbn("978-0000000001");
        libro.setTitulo("Harry Potter y la piedra filosofal");
        libro.setEditorial("Bloomsbury");
        libro.setAnioPublicacion(1997);
        libro.setDescripcion("Primer curso en Hogwarts");
        libro.setActivo(true);
        libro.setCategoria(categoria);
        libro.setAutores(new LinkedHashSet<>(autores));
        return libro;
    }

    private Autor autor(Long id, String nombreCompleto, boolean activo) {
        Autor autor = new Autor();
        autor.setId(id);
        autor.setNombreCompleto(nombreCompleto);
        autor.setActivo(activo);
        return autor;
    }
}