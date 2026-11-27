package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;

@Entity
@Filter(name = LibraryScope.LEGACY_FILTER)
@Table(
        name = "detalle_alquiler",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_detalle_alquiler_s_pair",
                        columnNames = {"scope_id", "alquiler_id", "ejemplar_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleAlquiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_id", nullable = false, updatable = false)
    @Builder.Default
    private long scopeId = LibraryScope.LEGACY_ID;

    @Column(
            name = "precio_dia",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precioDia;

    @Column(name = "cantidad_dias", nullable = false)
    private Integer cantidadDias;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "alquiler_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_detalle_alquiler")
    )
    private Alquiler alquiler;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ejemplar_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_detalle_ejemplar")
    )
    private Ejemplar ejemplar;
}