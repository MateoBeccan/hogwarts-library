package com.hogwarts.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@IdClass(SecuenciaCodigo.Clave.class)
@Table(name = "secuencia_codigo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuenciaCodigo {

    public record Clave(long scopeId, String nombre) {}

    @Id
    @Column(name = "scope_id", nullable = false)
    @Builder.Default
    private long scopeId = LibraryScope.LEGACY_ID;

    @Id
    @Column(length = 50, nullable = false)
    private String nombre;

    @Column(name = "ultimo_valor", nullable = false)
    private Long ultimoValor;

    public SecuenciaCodigo(String nombre, Long ultimoValor) {
        this(LibraryScope.LEGACY_ID, nombre, ultimoValor);
    }
}
