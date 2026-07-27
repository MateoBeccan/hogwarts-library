package com.hogwarts.library.controller;

import com.hogwarts.library.dto.pago.PagoRequest;
import com.hogwarts.library.dto.pago.PagoResponse;
import com.hogwarts.library.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> registrar(
            @Valid @RequestBody PagoRequest request
    ) {

        PagoResponse response = pagoService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> buscarPorId(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                pagoService.buscarPorId(id)
        );
    }

    @GetMapping("/alquiler/{alquilerId}")
    public ResponseEntity<List<PagoResponse>> listarPorAlquiler(
            @PathVariable Long alquilerId
    ) {

        return ResponseEntity.ok(
                pagoService.listarPorAlquiler(alquilerId)
        );
    }
}