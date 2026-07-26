package com.hogwarts.library.controller;

import com.hogwarts.library.dto.ejemplar.EjemplarRequest;
import com.hogwarts.library.dto.ejemplar.EjemplarResponse;
import com.hogwarts.library.service.EjemplarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ejemplares")
public class EjemplarController {

    private final EjemplarService ejemplarService;

    public EjemplarController(EjemplarService ejemplarService) {
        this.ejemplarService = ejemplarService;
    }

    @PostMapping
    public ResponseEntity<EjemplarResponse> crear(
            @Valid @RequestBody EjemplarRequest request
    ) {

        EjemplarResponse response = ejemplarService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EjemplarResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ejemplarService.buscarPorId(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<EjemplarResponse>> listar() {
        return ResponseEntity.ok(
                ejemplarService.listar()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<EjemplarResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EjemplarRequest request
    ) {
        return ResponseEntity.ok(
                ejemplarService.actualizar(id, request)
        );
    }

    @PatchMapping("/{id}/fuera-servicio")
    public ResponseEntity<EjemplarResponse> marcarFueraServicio(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ejemplarService.marcarFueraServicio(id)
        );
    }

    @PatchMapping("/{id}/habilitar")
    public ResponseEntity<EjemplarResponse> habilitar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ejemplarService.habilitar(id)
        );
    }
}