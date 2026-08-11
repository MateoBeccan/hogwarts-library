package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "libro",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_libro_isbn", columnNames = "isbn")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 150)
    private String editorial;

    @Column(name = "anio_publicacion")
    private Integer anioPublicacion;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "categoria_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_libro_categoria")
    )
    private Categoria categoria;

    @ManyToMany
    @JoinTable(
            name = "libro_autor",
            joinColumns = @JoinColumn(
                    name = "libro_id",
                    foreignKey = @ForeignKey(name = "fk_libro_autor_libro")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "autor_id",
                    foreignKey = @ForeignKey(name = "fk_libro_autor_autor")
            ),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_libro_autor",
                            columnNames = {"libro_id", "autor_id"}
                    )
            }
    )
    @Builder.Default
    private Set<Autor> autores = new LinkedHashSet<>();

    @OneToMany(mappedBy = "libro")
    @Builder.Default
    private List<Ejemplar> ejemplares = new ArrayList<>();
}