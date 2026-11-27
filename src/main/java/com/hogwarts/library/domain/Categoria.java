package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Filter(name = LibraryScope.LEGACY_FILTER)
@Table(
        name = "categoria",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categoria_s_nombre", columnNames = {"scope_id", "nombre"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_id", nullable = false, updatable = false)
    @Builder.Default
    private long scopeId = LibraryScope.LEGACY_ID;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activa = true;

    @OneToMany(mappedBy = "categoria")
    @Builder.Default
    private List<Libro> libros = new ArrayList<>();
}