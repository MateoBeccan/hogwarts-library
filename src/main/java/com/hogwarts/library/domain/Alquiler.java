package com.hogwarts.library.domain;

import com.hogwarts.library.domain.enums.EstadoAlquiler;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Filter(name = LibraryScope.LEGACY_FILTER)
@Table(name = "alquiler")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alquiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_id", nullable = false, updatable = false)
    @Builder.Default
    private long scopeId = LibraryScope.LEGACY_ID;

    @Column(name = "fecha_alquiler", nullable = false)
    private LocalDate fechaAlquiler;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_devolucion")
    private LocalDate fechaDevolucion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAlquiler estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cliente_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_alquiler_cliente")
    )
    private Cliente cliente;

    @OneToMany(
            mappedBy = "alquiler",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<DetalleAlquiler> detalles = new ArrayList<>();

    @OneToMany(
            mappedBy = "alquiler",
            cascade = CascadeType.ALL
    )
    @Builder.Default
    private List<Pago> pagos = new ArrayList<>();

    public void agregarDetalle(DetalleAlquiler detalle) {
        if (detalle.getEjemplar().getScopeId() != scopeId) {
            throw new IllegalArgumentException("El ejemplar pertenece a otro ámbito");
        }
        detalle.setScopeId(scopeId);
        detalle.setAlquiler(this);
        detalles.add(detalle);
    }
}