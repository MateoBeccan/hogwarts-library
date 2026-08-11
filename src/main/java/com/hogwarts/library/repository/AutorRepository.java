package com.hogwarts.library.repository;

import com.hogwarts.library.domain.Autor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    Optional<Autor> findByNombreCompletoIgnoreCase(String nombreCompleto);

    boolean existsByNombreCompletoIgnoreCase(String nombreCompleto);

    boolean existsByNombreCompletoIgnoreCaseAndIdNot(String nombreCompleto, Long id);
}