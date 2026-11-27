package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

@Entity
@Filter(name = LibraryScope.LEGACY_FILTER)
@Table(
        name = "autor",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_autor_s_nombre_completo", columnNames = {"scope_id", "nombre_completo"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Autor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_id", nullable = false, updatable = false)
    @Builder.Default
    private long scopeId = LibraryScope.LEGACY_ID;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}