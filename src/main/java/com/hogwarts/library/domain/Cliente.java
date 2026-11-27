package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Filter(name = LibraryScope.LEGACY_FILTER)
@Table(
        name = "cliente",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cliente_s_dni", columnNames = {"scope_id", "dni"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_id", nullable = false, updatable = false)
    @Builder.Default
    private long scopeId = LibraryScope.LEGACY_ID;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 20)
    private String dni;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "cliente")
    @Builder.Default
    private List<Alquiler> alquileres = new ArrayList<>();
}