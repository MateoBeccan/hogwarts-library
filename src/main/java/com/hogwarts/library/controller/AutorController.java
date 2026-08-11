package com.hogwarts.library.controller;

import com.hogwarts.library.dto.autor.AutorRequest;
import com.hogwarts.library.dto.autor.AutorResponse;
import com.hogwarts.library.service.AutorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/autores")
public class AutorController {

    private final AutorService autorService;

    public AutorController(AutorService autorService) {
        this.autorService = autorService;
    }

    @PostMapping
    public ResponseEntity<AutorResponse> crear(
            @Valid @RequestBody AutorRequest request
    ) {
        AutorResponse response = autorService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AutorResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(autorService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<AutorResponse>> listar() {
        return ResponseEntity.ok(autorService.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AutorResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AutorRequest request
    ) {
        return ResponseEntity.ok(autorService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        autorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}