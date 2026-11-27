package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Filter(name = LibraryScope.LEGACY_FILTER)
@Table(
        name = "libro",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_libro_s_isbn", columnNames = {"scope_id", "isbn"})
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

    @Column(name = "scope_id", nullable = false, updatable = false)
    @Builder.Default
    private long scopeId = LibraryScope.LEGACY_ID;

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

    @OneToMany(mappedBy = "libro", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Set<LibroAutor> vinculos = new LinkedHashSet<>();

    @OneToMany(mappedBy = "libro")
    @Builder.Default
    private List<Ejemplar> ejemplares = new ArrayList<>();

    public Set<Autor> getAutores() {
        Set<Autor> autores = new LinkedHashSet<>();
        for (LibroAutor vinculo : vinculos) {
            autores.add(vinculo.getAutor());
        }
        return autores;
    }

    public void setAutores(Set<Autor> autores) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Autor autor : autores) {
            if (autor.getScopeId() != scopeId) {
                throw new IllegalArgumentException("El autor pertenece a otro ámbito");
            }
            ids.add(autor.getId());
        }
        vinculos.removeIf(vinculo -> !ids.remove(vinculo.getAutor().getId()));
        for (Autor autor : autores) {
            if (ids.remove(autor.getId())) {
                vinculos.add(new LibroAutor(this, autor));
            }
        }
    }
}