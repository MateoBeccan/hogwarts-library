package com.hogwarts.library.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.FilterDef;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "library_scope")
@FilterDef(name = LibraryScope.LEGACY_FILTER, defaultCondition = "scope_id = " + LibraryScope.LEGACY_ID,
        autoEnabled = true, applyToLoadByKey = true)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LibraryScope {

    public static final long LEGACY_ID = 1L;
    public static final String LEGACY_FILTER = "legacyBusinessScope";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12)
    private String tipo;

    @Column(name = "public_id", length = 32, columnDefinition = "CHAR(32)")
    private String publicId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(nullable = false)
    private int generacion;

    @Column(name = "fecha_negocio")
    private LocalDate fechaNegocio;

    @Column(name = "version_semilla", length = 32)
    private String versionSemilla;

    @Column(nullable = false)
    private int mutaciones;

    @Column(nullable = false)
    private int restauraciones;

    @Column(name = "ultima_restauracion_at")
    private LocalDateTime ultimaRestauracionAt;
}
