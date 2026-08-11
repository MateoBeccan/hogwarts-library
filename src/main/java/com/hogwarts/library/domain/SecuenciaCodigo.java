package com.hogwarts.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "secuencia_codigo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuenciaCodigo {

    @Id
    @Column(length = 50, nullable = false)
    private String nombre;

    @Column(name = "ultimo_valor", nullable = false)
    private Long ultimoValor;
}
