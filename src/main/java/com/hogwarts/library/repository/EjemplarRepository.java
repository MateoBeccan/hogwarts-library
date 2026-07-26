package com.hogwarts.library.repository;

import com.hogwarts.library.domain.Ejemplar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EjemplarRepository extends JpaRepository<Ejemplar, Long> {

    Optional<Ejemplar> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}