package com.hogwarts.library.repository;

import com.hogwarts.library.domain.Alquiler;
import com.hogwarts.library.domain.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByAlquilerIdOrderByFechaAsc(Long alquilerId);

    @Query("""
       select coalesce(sum(p.monto), 0)
       from Pago p
       where p.alquiler.id = :alquilerId
       """)
    BigDecimal calcularTotalPagado(
            @Param("alquilerId") Long alquilerId
    );

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