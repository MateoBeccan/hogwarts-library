package com.hogwarts.library.domain;

import com.hogwarts.library.domain.enums.EstadoEjemplar;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Filter(name = LibraryScope.LEGACY_FILTER)
@Table(
        name = "ejemplar",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ejemplar_s_codigo", columnNames = {"scope_id", "codigo"})
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

    @Column(name = "scope_id", nullable = false, updatable = false)
    @Builder.Default
    private long scopeId = LibraryScope.LEGACY_ID;

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