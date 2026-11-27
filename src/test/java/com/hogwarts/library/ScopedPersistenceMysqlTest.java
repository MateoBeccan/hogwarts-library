package com.hogwarts.library;

import com.hogwarts.library.domain.*;
import com.hogwarts.library.domain.enums.EstadoAlquiler;
import com.hogwarts.library.domain.enums.EstadoEjemplar;
import com.hogwarts.library.domain.enums.MetodoPago;
import com.hogwarts.library.dto.alquiler.AlquilerRequest;
import com.hogwarts.library.dto.ejemplar.EjemplarRequest;
import com.hogwarts.library.dto.libro.LibroRequest;
import com.hogwarts.library.dto.pago.PagoRequest;
import com.hogwarts.library.repository.*;
import com.hogwarts.library.service.*;
import com.hogwarts.library.service.impl.EjemplarCodigoGenerator;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("migration")
class ScopedPersistenceMysqlTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired LibroRepository libros;
    @Autowired AutorRepository autores;
    @Autowired AlquilerRepository alquileres;
    @Autowired PagoRepository pagos;
    @Autowired LibraryScopeRepository scopes;
    @Autowired SecuenciaCodigoRepository secuencias;
    @Autowired EjemplarCodigoGenerator generator;
    @Autowired LibroService libroService;
    @Autowired EjemplarService ejemplarService;
    @Autowired AlquilerService alquilerService;
    @Autowired PagoService pagoService;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    void rejectsCrossScopeReferencesAndDuplicatesInSql() {
        insertScopes(2, 3);
        for (int scope : List.of(2, 3)) {
            long id = 100_000 + scope;
            jdbc.update("INSERT INTO categoria (id, scope_id, nombre) VALUES (?, ?, 'Compartida')", id, scope);
            jdbc.update("INSERT INTO autor (id, scope_id, nombre_completo) VALUES (?, ?, 'Mismo Autor')", id, scope);
            jdbc.update("""
                    INSERT INTO libro (id, scope_id, isbn, titulo, categoria_id)
                    VALUES (?, ?, 'ISBN-SCOPED', 'Título', ?)
                    """, id, scope, id);
            jdbc.update("""
                    INSERT INTO cliente (id, scope_id, dni, nombre, apellido, email, fecha_registro)
                    VALUES (?, ?, 'DNI-SCOPED', 'Nombre', 'Apellido', 'test@example.invalid', CURRENT_DATE)
                    """, id, scope);
            jdbc.update("""
                    INSERT INTO ejemplar (id, scope_id, libro_id, codigo, estado, precio_dia, fecha_alta)
                    VALUES (?, ?, ?, 'HOG-SCOPED', 'DISPONIBLE', 10, CURRENT_DATE)
                    """, id, scope, id);
            jdbc.update("""
                    INSERT INTO alquiler (id, scope_id, cliente_id, estado, fecha_alquiler, fecha_vencimiento, total)
                    VALUES (?, ?, ?, 'ACTIVO', CURRENT_DATE, CURRENT_DATE, 10)
                    """, id, scope, id);
            jdbc.update("INSERT INTO libro_autor (scope_id, libro_id, autor_id) VALUES (?, ?, ?)", scope, id, id);
        }

        rejects(1062, "INSERT INTO categoria (scope_id, nombre) VALUES (2, 'Compartida')");
        rejects(1062, "INSERT INTO autor (scope_id, nombre_completo) VALUES (2, 'Mismo Autor')");
        rejects(1062, "INSERT INTO libro (scope_id, isbn, titulo, categoria_id) VALUES (2, 'ISBN-SCOPED', 'Otro', 100002)");
        rejects(1062, """
                INSERT INTO cliente (scope_id, dni, nombre, apellido, email, fecha_registro)
                VALUES (2, 'DNI-SCOPED', 'Otro', 'Apellido', 'test@example.invalid', CURRENT_DATE)
                """);
        rejects(1062, """
                INSERT INTO ejemplar (scope_id, libro_id, codigo, estado, precio_dia, fecha_alta)
                VALUES (2, 100002, 'HOG-SCOPED', 'DISPONIBLE', 10, CURRENT_DATE)
                """);

        rejects(1452, "INSERT INTO categoria (scope_id, nombre) VALUES (999, 'Sin ámbito')");
        rejects(1452, "UPDATE libro SET categoria_id = 100003 WHERE id = 100002");
        rejects(1452, "UPDATE ejemplar SET libro_id = 100003 WHERE id = 100002");
        rejects(1452, "UPDATE alquiler SET cliente_id = 100003 WHERE id = 100002");
        rejects(1452, "INSERT INTO libro_autor (scope_id, libro_id, autor_id) VALUES (2, 100002, 100003)");
        rejects(1452, "INSERT INTO libro_autor (scope_id, libro_id, autor_id) VALUES (2, 100003, 100002)");
        rejects(1452, """
                INSERT INTO detalle_alquiler (scope_id, alquiler_id, ejemplar_id, cantidad_dias, precio_dia, subtotal)
                VALUES (2, 100002, 100003, 1, 10, 10)
                """);
        rejects(1452, """
                INSERT INTO detalle_alquiler (scope_id, alquiler_id, ejemplar_id, cantidad_dias, precio_dia, subtotal)
                VALUES (2, 100003, 100002, 1, 10, 10)
                """);
        rejects(1452, """
                INSERT INTO pago (scope_id, alquiler_id, fecha, metodo_pago, monto)
                VALUES (2, 100003, CURRENT_TIMESTAMP, 'EFECTIVO', 10)
                """);

        assertThat(libros.findById(100002L)).isEmpty();
        assertThat(libros.existsById(100002L)).isFalse();
        assertThat(libros.findByIsbn("ISBN-SCOPED")).isEmpty();
        assertThat(libros.findAll()).noneMatch(libro -> libro.getScopeId() != 1);
        assertThat(autores.findAllById(Set.of(100002L, 100003L))).isEmpty();
        assertThat(alquileres.findByIdForUpdate(100002L)).isEmpty();
        assertThat(alquileres.findByClienteId(100002L)).isEmpty();
        jdbc.update("""
                INSERT INTO pago (scope_id, alquiler_id, fecha, metodo_pago, monto)
                VALUES (2, 100002, CURRENT_TIMESTAMP, 'EFECTIVO', 10)
                """);
        assertThat(pagos.findByAlquilerIdOrderByFechaAsc(100002L)).isEmpty();
        assertThat(pagos.calcularTotalPagado(100002L)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @Transactional
    void mapsNonLegacyBookLinksAndRentalDetailsThroughFlushAndReload() {
        insertScopes(2);
        // The prototype uses one scope in a dedicated test transaction.
        entityManager.unwrap(Session.class).disableFilter(LibraryScope.LEGACY_FILTER);
        Categoria categoria = new Categoria();
        categoria.setScopeId(2);
        categoria.setNombre("Prueba JPA");
        entityManager.persist(categoria);

        Autor autor = persistAuthor(2, "Autor JPA");
        Autor segundoAutor = persistAuthor(2, "Segundo autor JPA");
        Autor autorAjeno = persistAuthor(1, "Autor LEGACY JPA");
        Libro libro = new Libro();
        libro.setScopeId(2);
        libro.setTitulo("Libro JPA");
        libro.setIsbn("ISBN-JPA");
        libro.setCategoria(categoria);
        assertThatIllegalArgumentException().isThrownBy(() -> libro.setAutores(Set.of(autor, autorAjeno)));
        libro.setAutores(Set.of(autor, segundoAutor));
        entityManager.persist(libro);

        Cliente cliente = new Cliente();
        cliente.setScopeId(2);
        cliente.setNombre("Cliente");
        cliente.setApellido("JPA");
        cliente.setDni("DNI-JPA");
        cliente.setEmail("cliente@example.invalid");
        cliente.setFechaRegistro(LocalDate.now());
        entityManager.persist(cliente);

        Ejemplar ejemplar = Ejemplar.builder().scopeId(2).libro(libro).codigo("HOG-JPA")
                .estado(EstadoEjemplar.DISPONIBLE)
                .precioDia(BigDecimal.TEN).fechaAlta(LocalDate.now()).build();
        entityManager.persist(ejemplar);
        Alquiler alquiler = Alquiler.builder().scopeId(2).cliente(cliente).estado(EstadoAlquiler.ACTIVO)
                .fechaAlquiler(LocalDate.now()).fechaVencimiento(LocalDate.now().plusDays(1))
                .total(BigDecimal.TEN).build();
        DetalleAlquiler detalle = DetalleAlquiler.builder().ejemplar(ejemplar)
                .cantidadDias(1).precioDia(BigDecimal.TEN).subtotal(BigDecimal.TEN).build();
        alquiler.agregarDetalle(detalle);
        entityManager.persist(alquiler);

        entityManager.flush();
        entityManager.clear();
        Libro recargado = libros.findById(libro.getId()).orElseThrow();
        assertThat(recargado.getAutores()).extracting(Autor::getId)
                .containsExactlyInAnyOrder(autor.getId(), segundoAutor.getId());
        recargado.setAutores(Set.of(entityManager.getReference(Autor.class, segundoAutor.getId())));
        Alquiler alquilerRecargado = alquileres.findByIdForUpdate(alquiler.getId()).orElseThrow();
        assertThat(alquilerRecargado.getDetalles()).singleElement().satisfies(guardado -> {
            assertThat(guardado.getScopeId()).isEqualTo(2);
            assertThat(guardado.getEjemplar().getId()).isEqualTo(ejemplar.getId());
        });
        alquilerRecargado.getDetalles().clear();
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbc.queryForList("SELECT scope_id, autor_id FROM libro_autor WHERE libro_id = ?", libro.getId()))
                .singleElement().satisfies(row -> {
                    assertThat(((Number) row.get("scope_id")).longValue()).isEqualTo(2);
                    assertThat(((Number) row.get("autor_id")).longValue()).isEqualTo(segundoAutor.getId());
                });
        assertThat(entityManager.find(DetalleAlquiler.class, detalle.getId())).isNull();
        assertThat(entityManager.find(Ejemplar.class, ejemplar.getId())).isNotNull();
    }

    @Test
    @Transactional
    void legacyServicesPersistBookRentalAndPaymentUsingTheirNormalEntryPoints() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Categoría de regresión");
        entityManager.persist(categoria);
        Autor autor = persistAuthor(1, "Autor de regresión");
        Autor reemplazo = persistAuthor(1, "Reemplazo de regresión");

        var libro = libroService.crear(new LibroRequest("ISBN-LEGACY", "Libro de regresión",
                List.of(autor.getId()), null, null, null, categoria.getId()));
        entityManager.flush();
        entityManager.clear();
        var editado = libroService.actualizar(libro.id(), new LibroRequest("ISBN-LEGACY", "Título editado",
                List.of(autor.getId(), reemplazo.getId()), null, null, null, categoria.getId()));
        assertThat(editado.autores()).hasSize(2);
        entityManager.flush();
        entityManager.clear();

        var ejemplar = ejemplarService.crear(new EjemplarRequest(BigDecimal.TEN, null, libro.id()));
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente");
        cliente.setApellido("Regresión");
        cliente.setDni("DNI-LEGACY");
        cliente.setEmail("regresion@example.invalid");
        cliente.setFechaRegistro(LocalDate.now());
        entityManager.persist(cliente);

        var alquiler = alquilerService.crear(new AlquilerRequest(cliente.getId(), LocalDate.now().plusDays(2),
                List.of(ejemplar.id()), null));
        pagoService.registrar(new PagoRequest(alquiler.id(), BigDecimal.ONE, MetodoPago.EFECTIVO, null));
        entityManager.flush();
        entityManager.clear();

        assertThat(libroService.buscarPorId(libro.id()).autores()).hasSize(2);
        assertThat(alquilerService.buscarPorId(alquiler.id()).detalles()).hasSize(1);
        assertThat(pagoService.listarPorAlquiler(alquiler.id())).hasSize(1);
        assertThat(ejemplarService.buscarPorId(ejemplar.id()).estado()).isEqualTo(EstadoEjemplar.ALQUILADO);
    }

    @Test
    @Transactional
    void legacyGeneratorUsesTheExistingCounter() {
        long previous = jdbc.queryForObject("""
                SELECT ultimo_valor FROM secuencia_codigo WHERE scope_id = 1 AND nombre = 'EJEMPLAR'
                """, Long.class);
        assertThat(generator.generarSiguienteCodigo()).isEqualTo(generator.formatear(previous + 1));
        entityManager.flush();
        assertThat(secuencias.findByNombreForUpdate("EJEMPLAR").orElseThrow().getUltimoValor())
                .isEqualTo(previous + 1);
    }

    @Test
    void pessimisticLocksSerializeOneScopeButAllowAnotherToAdvance() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.executeWithoutResult(status -> {
            insertScopes(20, 21);
            jdbc.update("INSERT INTO secuencia_codigo VALUES (20, 'EJEMPLAR', 40), (21, 'EJEMPLAR', 80)");
        });
        try {
            assertLock(transaction, true);
            assertLock(transaction, false);
            assertThat(jdbc.queryForList("""
                    SELECT ultimo_valor FROM secuencia_codigo WHERE scope_id IN (20, 21) ORDER BY scope_id
                    """, Long.class)).containsExactly(44L, 82L);
        } finally {
            transaction.executeWithoutResult(status -> {
                jdbc.update("DELETE FROM secuencia_codigo WHERE scope_id IN (20, 21)");
                jdbc.update("DELETE FROM library_scope WHERE id IN (20, 21)");
            });
        }
    }

    private void assertLock(TransactionTemplate transaction, boolean lockScope) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(3)) {
            var holder = executor.submit(() -> transaction.executeWithoutResult(status -> {
                if (lockScope) {
                    scopes.findByIdForUpdate(20).orElseThrow();
                } else {
                    secuencias.findByScopeIdAndNombreForUpdate(20, "EJEMPLAR").orElseThrow();
                }
                locked.countDown();
                await(release);
                incrementSequence(20);
            }));
            try {
                await(locked);
                CountDownLatch attempting = new CountDownLatch(1);
                var waiter = executor.submit(() -> transaction.executeWithoutResult(status -> {
                    attempting.countDown();
                    if (lockScope) {
                        scopes.findByIdForUpdate(20).orElseThrow();
                    }
                    incrementSequence(20);
                }));
                await(attempting);
                var independent = executor.submit(() -> transaction.executeWithoutResult(status -> {
                    if (lockScope) {
                        scopes.findByIdForUpdate(21).orElseThrow();
                    }
                    incrementSequence(21);
                }));
                independent.get(5, TimeUnit.SECONDS);
                assertThatThrownBy(() -> waiter.get(250, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
                release.countDown();
                waiter.get(5, TimeUnit.SECONDS);
                holder.get(5, TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
        }
    }

    private void incrementSequence(long scopeId) {
        SecuenciaCodigo secuencia = secuencias.findByScopeIdAndNombreForUpdate(scopeId, "EJEMPLAR").orElseThrow();
        secuencia.setUltimoValor(secuencia.getUltimoValor() + 1);
    }

    private void insertScopes(long... ids) {
        for (long id : ids) {
            jdbc.update("""
                    INSERT INTO library_scope (id, tipo, public_id, created_at, expires_at)
                    VALUES (?, 'SANDBOX', REPLACE(UUID(), '-', ''), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) + INTERVAL 30 MINUTE)
                    """, id);
        }
    }

    private Autor persistAuthor(long scopeId, String nombre) {
        Autor autor = Autor.builder().scopeId(scopeId).nombreCompleto(nombre).build();
        entityManager.persist(autor);
        return autor;
    }

    private void rejects(int errorCode, String sql) {
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOfSatisfying(SQLException.class,
                        failure -> assertThat(failure.getErrorCode()).isEqualTo(errorCode));
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
