package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "categoria",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categoria_nombre", columnNames = "nombre")
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