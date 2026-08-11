package com.hogwarts.library.repository;

import com.hogwarts.library.domain.SecuenciaCodigo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SecuenciaCodigoRepository
        extends JpaRepository<SecuenciaCodigo, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       select s
       from SecuenciaCodigo s
       where s.nombre = :nombre
       """)
    Optional<SecuenciaCodigo> findByNombreForUpdate(
            @Param("nombre") String nombre
    );
}
