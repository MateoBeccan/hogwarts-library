package com.hogwarts.library.repository;

import com.hogwarts.library.domain.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

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

}