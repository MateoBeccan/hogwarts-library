package com.hogwarts.library;

import com.jayway.jsonpath.JsonPath;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScopedMigrationHistoryTest {

    private static final String CHANGELOG_PREFIX = "classpath:db/changelog/db.changelog-";
    private static final List<String> SCOPED_TABLES = List.of(
            "categoria", "autor", "libro", "libro_autor", "cliente", "ejemplar",
            "alquiler", "detalle_alquiler", "pago", "secuencia_codigo"
    );
    private static final Map<String, String> OLD_CHECKSUMS = Map.of(
            "003-create-libro", "9:a3a668dede57673bbcace23ee18fe3ca",
            "004-create-ejemplar", "9:cb2ca14f81e116857b4854188ec3f608",
            "005-create-alquiler", "9:9b1205e013441d95d2306b5cfcebbe5e",
            "006-create-detalle-alquiler", "9:a7f6acf571d4f8c417cc725f81415651",
            "007-create-pago", "9:d9035004e4772dc5e0218e284dfee552",
            "003-002-create-libro-autor", "9:4633e2bbcea85f4e2546386f2cf118a7"
    );

    @Test
    @EnabledIfEnvironmentVariable(named = "V231_PRIVATE_UPGRADE_URL", matches = ".+")
    void upgradesPrivateDataAndAcceptsTheSixOriginalChecksums() throws Exception {
        DriverManagerDataSource db = disposable("V231_PRIVATE_UPGRADE_URL", "hogwarts_library_upgrade_test");
        JdbcTemplate jdbc = new JdbcTemplate(db);
        migrate(db, "master");
        new ResourceDatabasePopulator(false, false, StandardCharsets.UTF_8.name(),
                new ClassPathResource("db/changelog/demo/fictitious-library.sql")).execute(db);
        jdbc.update("UPDATE secuencia_codigo SET ultimo_valor = 47 WHERE nombre = 'EJEMPLAR'");

        String password = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO usuario (id, username, password, nombre, apellido, rol)
                VALUES (17, 'private-fixture-admin', ?, 'Admin', 'Fixture', 'ADMIN'),
                       (29, 'private-fixture-empleado', ?, 'Empleado', 'Fixture', 'EMPLEADO')
                """, new BCryptPasswordEncoder().encode(password), new BCryptPasswordEncoder().encode(password));
        OLD_CHECKSUMS.forEach((id, checksum) ->
                assertThat(jdbc.update("UPDATE databasechangelog SET MD5SUM = ? WHERE ID = ?", checksum, id))
                        .isEqualTo(1));

        var rows = snapshot(jdbc);
        var history = history(jdbc);
        assertThat(history).hasSize(14);
        assertThatThrownBy(() -> migrate(db, "v2.3-public")).isInstanceOf(LiquibaseException.class);
        assertThat(history(jdbc)).isEqualTo(history);

        try (var context = boot(db, "migration")) {
            MockMvc mvc = mvc(context);
            String admin = privateBearer(mvc, "private-fixture-admin", password, "ADMIN");
            String empleado = privateBearer(mvc, "private-fixture-empleado", password, "EMPLEADO");
            mvc.perform(get("/api/libros/3").header("Authorization", empleado))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.autores.length()").value(2));
            mvc.perform(delete("/api/clientes/3").header("Authorization", empleado))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/clientes/3").header("Authorization", admin)).andExpect(status().isOk());
            mvc.perform(get("/healthz")).andExpect(status().isOk());
        }
        assertPreserved(jdbc, rows, history);
        assertThat(jdbc.queryForObject("SELECT ultimo_valor FROM secuencia_codigo WHERE scope_id = 1", Long.class))
                .isEqualTo(47L);
        assertRestart(db, "v2.3-private");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "V231_DEMO_UPGRADE_URL", matches = ".+")
    void upgradesTheExistingDemoSeedAndKeepsDemoReadOnly() throws Exception {
        DriverManagerDataSource db = disposable("V231_DEMO_UPGRADE_URL", "hogwarts_library_demo");
        JdbcTemplate jdbc = new JdbcTemplate(db);
        migrate(db, "demo");

        var rows = snapshot(jdbc);
        var history = history(jdbc);
        assertThat(history).hasSize(15);
        assertDemo(db);
        assertPreserved(jdbc, rows, history);
        assertRestart(db, "v2.3-public");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "V231_DEMO_FRESH_URL", matches = ".+")
    void installsTheDemoFromEmptyAndSeedsBeforeRequiringScope() throws Exception {
        DriverManagerDataSource db = disposable("V231_DEMO_FRESH_URL", "hogwarts_library_demo");
        JdbcTemplate jdbc = new JdbcTemplate(db);
        assertDemo(db);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM libro_autor WHERE scope_id = 1", Long.class))
                .isEqualTo(4L);
        assertThat(jdbc.queryForObject("SELECT ultimo_valor FROM secuencia_codigo WHERE scope_id = 1", Long.class))
                .isEqualTo(6L);
        assertThat(history(jdbc)).filteredOn(row -> "demo-001-fictitious-library".equals(row.get("ID"))).hasSize(1);
        assertRestart(db, "v2.3-public");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "V231_PARTIAL_DDL_URL", matches = ".+")
    void resumesAfterReconcilingAnAdditiveDdlThatWasNotRecorded() throws Exception {
        DriverManagerDataSource db = disposable("V231_PARTIAL_DDL_URL", "hogwarts_library_partial_ddl_test");
        JdbcTemplate jdbc = new JdbcTemplate(db);
        migrate(db, "master");
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (42, 'Registro previo')");
        var before = snapshot(jdbc);
        var history = history(jdbc);

        // Simulate a committed DDL followed by loss of the Liquibase history write.
        jdbc.execute("ALTER TABLE autor ADD COLUMN scope_id BIGINT NULL");
        assertThatThrownBy(() -> migrate(db, "v2.3-private"))
                .isInstanceOf(LiquibaseException.class).hasStackTraceContaining("Duplicate column name");
        assertThat(history(jdbc)).noneMatch(row -> "004-column-autor".equals(row.get("ID")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM autor WHERE scope_id IS NOT NULL", Long.class)).isZero();

        // Only the unrecorded, empty column is reverted so Liquibase can run its own change.
        jdbc.execute("ALTER TABLE autor DROP COLUMN scope_id");
        migrate(db, "v2.3-private");
        assertPreserved(jdbc, before, history);
        assertRestart(db, "v2.3-private");
    }

    private void assertDemo(DriverManagerDataSource db) throws Exception {
        try (var context = boot(db, "demo")) {
            assertThat(context.getEnvironment().getProperty("app.sandbox.enabled", Boolean.class)).isFalse();
            if (db.getUrl().contains("sslMode=VERIFY_IDENTITY")) {
                assertThat(context.getBean(JdbcTemplate.class)
                        .queryForMap("SHOW SESSION STATUS LIKE 'Ssl_cipher'").get("Value").toString())
                        .isNotBlank();
            }
            MockMvc mvc = mvc(context);
            String response = mvc.perform(post("/api/auth/demo")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("DEMO"))
                    .andReturn().getResponse().getContentAsString();
            String bearer = "Bearer " + JsonPath.<String>read(response, "$.token");
            mvc.perform(get("/api/libros").header("Authorization", bearer))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
            mvc.perform(get("/api/libros/3").header("Authorization", bearer))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.autores.length()").value(2));
            mvc.perform(get("/api/alquileres/1").header("Authorization", bearer))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.detalles.length()").value(1));
            mvc.perform(post("/api/libros").header("Authorization", bearer)).andExpect(status().isForbidden());
            mvc.perform(post("/api/demo/sessions").header("Authorization", bearer)).andExpect(status().isForbidden());
            mvc.perform(get("/healthz")).andExpect(status().isOk());
        }
    }

    private static ConfigurableApplicationContext boot(DriverManagerDataSource db, String profile) {
        return new SpringApplicationBuilder(LibraryApplication.class)
                .profiles(profile).run("--server.port=0", "--server.address=127.0.0.1",
                        "--spring.datasource.url=" + (profile.equals("demo") ? db.getUrl().replace("sslMode=VERIFY_IDENTITY&", "") : db.getUrl()),
                        "--spring.datasource.username=" + db.getUsername(),
                        "--spring.datasource.password=" + db.getPassword(),
                        "--app.jwt.secret=" + UUID.randomUUID(),
                        "--app.cors.allowed-origins=https://demo.example.invalid",
                        "--spring.jpa.show-sql=false");
    }

    private static MockMvc mvc(ConfigurableApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).apply(springSecurity()).build();
    }

    private static String privateBearer(MockMvc mvc, String username, String password, String role) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value(role))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.<String>read(response, "$.token");
    }

    private static DriverManagerDataSource disposable(String variable, String schema) {
        String url = System.getenv(variable);
        assertThat(url).matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/" + schema + "(?:\\?[^\\r\\n]*)?");
        String password = System.getenv("DB_PASSWORD");
        var db = new DriverManagerDataSource(url, System.getenv("DB_USERNAME"), password == null ? "" : password);
        assertThat(new JdbcTemplate(db).queryForObject("""
                SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
                """, Long.class)).as("El esquema descartable debe empezar vacío").isZero();
        return db;
    }

    private static void migrate(DriverManagerDataSource db, String root) throws LiquibaseException {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(db);
        liquibase.setChangeLog(CHANGELOG_PREFIX + root + ".yaml");
        liquibase.afterPropertiesSet();
    }

    private static void assertPreserved(JdbcTemplate jdbc, Map<String, List<Map<String, Object>>> before,
                                        List<Map<String, Object>> history) {
        var after = snapshot(jdbc);
        for (String table : SCOPED_TABLES) {
            assertThat(after.get(table)).allSatisfy(row ->
                    assertThat(((Number) row.remove("scope_id")).longValue()).isEqualTo(1L));
        }
        assertThat(after).isEqualTo(before);
        assertThat(history(jdbc).subList(0, history.size())).isEqualTo(history);
    }

    private static Map<String, List<Map<String, Object>>> snapshot(JdbcTemplate jdbc) {
        var rows = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (String table : SCOPED_TABLES) {
            String order = switch (table) {
                case "libro_autor" -> "libro_id, autor_id";
                case "secuencia_codigo" -> "nombre";
                default -> "id";
            };
            rows.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY " + order));
        }
        rows.put("usuario", jdbc.queryForList("SELECT * FROM usuario ORDER BY id"));
        return rows;
    }

    private static List<Map<String, Object>> history(JdbcTemplate jdbc) {
        return jdbc.queryForList("SELECT * FROM databasechangelog ORDER BY ORDEREXECUTED");
    }

    private static void assertRestart(DriverManagerDataSource db, String root) throws LiquibaseException {
        JdbcTemplate jdbc = new JdbcTemplate(db);
        var before = history(jdbc);
        migrate(db, root);
        assertThat(history(jdbc)).isEqualTo(before);
    }
}
