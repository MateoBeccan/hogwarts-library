package com.hogwarts.library.repository;

import com.hogwarts.library.domain.Alquiler;
import com.hogwarts.library.domain.enums.EstadoAlquiler;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlquilerRepository extends JpaRepository<Alquiler, Long> {

    List<Alquiler> findByClienteId(Long clienteId);

    List<Alquiler> findByEstado(EstadoAlquiler estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select a
           from Alquiler a
           where a.id = :id
           """)
    Optional<Alquiler> findByIdForUpdate(
            @Param("id") Long id
    );
}