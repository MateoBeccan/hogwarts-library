package com.hogwarts.library.repository;

import com.hogwarts.library.domain.LibraryScope;
import com.hogwarts.library.domain.SecuenciaCodigo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SecuenciaCodigoRepository
        extends JpaRepository<SecuenciaCodigo, SecuenciaCodigo.Clave> {

    default Optional<SecuenciaCodigo> findByNombreForUpdate(String nombre) {
        return findByScopeIdAndNombreForUpdate(LibraryScope.LEGACY_ID, nombre);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       select s
       from SecuenciaCodigo s
       where s.scopeId = :scopeId and s.nombre = :nombre
       """)
    Optional<SecuenciaCodigo> findByScopeIdAndNombreForUpdate(
            @Param("scopeId") long scopeId,
            @Param("nombre") String nombre
    );
}
