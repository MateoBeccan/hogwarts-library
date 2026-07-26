package com.hogwarts.library.controller;

import com.hogwarts.library.dto.alquiler.AlquilerRequest;
import com.hogwarts.library.dto.alquiler.AlquilerResponse;
import com.hogwarts.library.service.AlquilerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alquileres")
public class AlquilerController {

    private final AlquilerService alquilerService;

    public AlquilerController(AlquilerService alquilerService) {
        this.alquilerService = alquilerService;
    }

    @PostMapping
    public ResponseEntity<AlquilerResponse> crear(
            @Valid @RequestBody AlquilerRequest request
    ) {

        AlquilerResponse response = alquilerService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlquilerResponse> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                alquilerService.buscarPorId(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<AlquilerResponse>> listar() {
        return ResponseEntity.ok(
                alquilerService.listar()
        );
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<AlquilerResponse>> listarPorCliente(
            @PathVariable Long clienteId
    ) {
        return ResponseEntity.ok(
                alquilerService.listarPorCliente(clienteId)
        );
    }

    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<AlquilerResponse> finalizar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                alquilerService.finalizar(id)
        );
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AlquilerResponse> cancelar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                alquilerService.cancelar(id)
        );
    }
}