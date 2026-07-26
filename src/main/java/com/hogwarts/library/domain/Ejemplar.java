package com.hogwarts.library.domain;

import com.hogwarts.library.domain.enums.EstadoEjemplar;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ejemplar",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ejemplar_codigo", columnNames = "codigo")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ejemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoEjemplar estado;

    @Column(
            name = "precio_dia",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precioDia;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "libro_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ejemplar_libro")
    )
    private Libro libro;

    @OneToMany(mappedBy = "ejemplar")
    @Builder.Default
    private List<DetalleAlquiler> detallesAlquiler = new ArrayList<>();
}