package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "autor",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_autor_nombre_completo", columnNames = "nombre_completo")
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

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @ManyToMany(mappedBy = "autores")
    @Builder.Default
    private Set<Libro> libros = new LinkedHashSet<>();
}