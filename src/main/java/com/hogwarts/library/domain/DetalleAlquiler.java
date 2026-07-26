package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "detalle_alquiler",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_detalle_alquiler_ejemplar",
                        columnNames = {"alquiler_id", "ejemplar_id"}
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