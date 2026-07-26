package com.hogwarts.library.repository;

import com.hogwarts.library.domain.Alquiler;
import com.hogwarts.library.domain.enums.EstadoAlquiler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlquilerRepository extends JpaRepository<Alquiler, Long> {

    List<Alquiler> findByClienteId(Long clienteId);

    List<Alquiler> findByEstado(EstadoAlquiler estado);
}