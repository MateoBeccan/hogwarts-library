package com.hogwarts.library.dto.cliente;

import java.time.LocalDate;

public record ClienteResponse(
        Long id,
        String nombre,
        String apellido,
        String dni,
        String email,
        String telefono,
        LocalDate fechaRegistro,
        Boolean activo
) {
}