package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "libro_autor")
@Filter(name = LibraryScope.LEGACY_FILTER)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LibroAutor {

    @Embeddable
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Id {
        @Column(name = "scope_id", nullable = false)
        private long scopeId;

        @Column(name = "libro_id", nullable = false)
        private Long libroId;

        @Column(name = "autor_id", nullable = false)
        private Long autorId;
    }

    @EmbeddedId
    private Id id;

    @MapsId("libroId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false, foreignKey = @ForeignKey(name = "fk_libro_autor_libro"))
    private Libro libro;

    @MapsId("autorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_libro_autor_autor"))
    @Getter
    private Autor autor;

    LibroAutor(Libro libro, Autor autor) {
        this.id = new Id(libro.getScopeId(), libro.getId(), autor.getId());
        this.libro = libro;
        this.autor = autor;
    }
}
