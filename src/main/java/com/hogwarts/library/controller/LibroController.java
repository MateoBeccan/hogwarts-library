package com.hogwarts.library.controller;

import com.hogwarts.library.dto.libro.LibroRequest;
import com.hogwarts.library.dto.libro.LibroResponse;
import com.hogwarts.library.service.LibroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @PostMapping
    public ResponseEntity<LibroResponse> crear(
            @Valid @RequestBody LibroRequest request
    ) {

        LibroResponse response = libroService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibroResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                libroService.buscarPorId(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<LibroResponse>> listar() {
        return ResponseEntity.ok(
                libroService.listar()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<LibroResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody LibroRequest request
    ) {
        return ResponseEntity.ok(
                libroService.actualizar(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @PathVariable Long id
    ) {

        libroService.desactivar(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<LibroResponse> reactivar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                libroService.reactivar(id)
        );
    }
}