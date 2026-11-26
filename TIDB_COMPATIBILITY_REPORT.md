# Compatibilidad de Liquibase con TiDB Cloud Starter

El arranque informado en TiDB v8.5.3 Serverless llegó a Liquibase con Connector/J y TLS. Al agregar `fk_libro_categoria`, TiDB creó automáticamente un índice del mismo nombre en la tabla hija. El siguiente `CREATE INDEX` devolvió `Duplicate key name 'fk_libro_categoria'`. El DDL anterior al error quedó aplicado y Liquibase solo registró los dos changesets completados. Reintentar sobre ese esquema puede fallar al volver a crear `libro`. [Claves foráneas en TiDB](https://docs.pingcap.com/tidb/v8.5/foreign-key/).

## Migraciones y configuración auditadas

| Archivo o área | Hallazgo y decisión |
| --- | --- |
| `db.changelog-master.yaml` | El maestro incluye 001, 002 y 003 en orden. La demo lo reutiliza, con 14 changesets comunes y uno exclusivo. |
| `001-baseline-v1.yaml` | En `003-create-libro`, `004-create-ejemplar`, `006-create-detalle-alquiler` y `007-create-pago` el índice explícito tenía el nombre que TiDB asigna al índice implícito. En `005-create-alquiler`, el índice compuesto `idx_alquiler_cliente_estado` cubre `cliente_id` como primera columna. En `006`, la restricción única de `(alquiler_id, ejemplar_id)` ya cubre la FK de `alquiler_id`. Los índices y restricciones únicas se crean antes de agregar las FK. |
| `002-auto-ejemplar-code.yaml` | La secuencia del dominio usa una tabla con PK `VARCHAR` y contador `BIGINT`. Su inicialización usa `COALESCE`, `MAX`, `REGEXP`, `SUBSTRING` y `CAST AS UNSIGNED` sobre códigos con un patrón ASCII. El bloque `dbms: mysql` depende de la identificación de Connector/J. La semilla actualiza el contador a 6. |
| `003-normalize-autores.yaml` | En `003-002-create-libro-autor`, las dos FK precedían a los índices y podían dejar índices implícitos adicionales. Ahora el índice único y ambos índices explícitos preceden a las FK. El backfill usa `INSERT … SELECT DISTINCT`, `CASE`, `TRIM` y un join. Se inserta primero en `autor` y después en `libro_autor`. El `DROP COLUMN` de `libro.autor` se ejecuta al final y no afecta una columna indexada o referenciada. |
| DDL, tipos y restricciones | Los `ALTER TABLE` agregan restricciones únicas o FK de una en una y eliminan una columna al final. Las FK referencian PK `BIGINT` con tipos coincidentes. Los IDs autoincrementales son PK y el dominio no requiere IDs consecutivos. El esquema usa `BIT`, `INT`, `BIGINT`, `VARCHAR`, `DECIMAL`, `DATE` y `DATETIME(6)`. No hay `DROP INDEX`, restricciones SQL `CHECK` ni cambios de tipo de columna en estos changelogs. TiDB tiene restricciones al retirar índices que soportan FK y sus `CHECK` requieren configuración propia si se agregan en el futuro. [Compatibilidad MySQL](https://docs.pingcap.com/tidbcloud/mysql-compatibility/), [restricciones de TiDB](https://docs.pingcap.com/tidb/v8.5/constraints/). |
| `db.changelog-demo.yaml` y `fictitious-library.sql` | La precondición global requiere el motor MySQL de Liquibase y el nombre de esquema exacto con `BINARY DATABASE()`. La precondición de la semilla exige tablas del dominio vacías. La semilla respeta el orden padre–hijo, asigna IDs explícitos y calcula fechas con `CURRENT_DATE` e `INTERVAL`. Solo usa datos ficticios. La precondición y el archivo de datos conservan su contenido y su historial. |
| Perfiles, JDBC e Hibernate | `dev` conserva MySQL local. `demo` activa `hosted`, que exige variables de conexión, TLS `VERIFY_IDENTITY` y orígenes CORS HTTPS explícitos. Hibernate conserva `ddl-auto=validate` y selecciona su dialecto por los metadatos de Connector/J. La validación remota debe confirmar esos metadatos, las funciones SQL, las restricciones y el comportamiento del lock de Liquibase. [Connector/J en TiDB](https://docs.pingcap.com/developer/dev-guide-sample-application-java-jdbc/). |

## Historial y estrategia

Las seis ediciones conservan el ID, autor, ruta y operaciones de cada changeset. Solo cambia el orden del DDL. Cada uno declara un `validCheckSum` exacto de la versión 9, obtenido al ejecutar los archivos originales con Liquibase 5.0.3 sobre MySQL 8.0.43. El historial nuevo registra el checksum de la versión reordenada. Liquibase acepta el checksum histórico y omite el DDL ya aplicado en instalaciones MySQL existentes.

Un changelog específico para TiDB duplicaría el esquema y requeriría mantener dos secuencias de evolución. Una precondición que omitiera un changeset completo dejaría sin comprobar el resto de su DDL tras un fallo parcial. Una migración posterior tampoco alcanzaría a corregir el primer arranque porque el error ocurre antes. La corrección comparte un único changelog, preserva las ocho FK y los índices explícitos y mantiene la integridad referencial habilitada. [Checksums de Liquibase](https://docs.liquibase.com/community/user-guide-5-0/what-is-a-changeset-checksum).

## Archivos y validación

- `001-baseline-v1.yaml` y `003-normalize-autores.yaml` contienen el nuevo orden y los seis checksums históricos.
- `ForeignKeyIndexOrderTest.java` comprueba en el changelog real que cada FK tenga un índice explícito o una restricción única que empiece por la columna hija antes de su creación.
- `DEPLOYMENT_GUIDE.md`, `DEMO_READINESS_REPORT.md` y este informe registran la alternativa gratuita, el diagnóstico y el procedimiento para TiDB.

| Validación | Resultado |
| --- | --- |
| `mvn -o -B verify` con un MySQL 8.0.43 descartable | 74 pruebas superadas, 0 fallos, 0 errores, 0 omitidas. Incluye las 73 existentes, el arranque de Liquibase e Hibernate sobre MySQL vacío y la nueva prueba de orden. |
| MySQL con historial anterior | El JAR arrancó en `hosted` con TLS y Hibernate `validate`. Liquibase ejecutó 0 changesets y conservó las 14 filas de historial con sus checksums, estados y fechas originales. |
| Esquema demo vacío y reinicio sobre MySQL | El primer arranque con TLS aplicó 15 changesets y el segundo aplicó 0. Se comprobaron los conteos de toda la semilla, el contador, una única carga demo, las ocho FK, el rechazo de una referencia huérfana y la ausencia de índices con listas de columnas idénticas. El historial MySQL anterior también conservó sus ocho FK e índices sin firmas duplicadas. |
| API demo sobre MySQL | `/healthz` devolvió 200 y `UP`. La API emitió un JWT `DEMO` con `Cache-Control: no-store`, permitió leer el catálogo con 200 y devolvió 401 sin token y 403 al intentar escribir con el token demo. |
| TiDB Cloud v8.5.3 Serverless | Pendiente. El entorno no tenía variables con la conexión remota y el repositorio solo aportaba un archivo de ejemplo. El estado y contenido exactos de la base remota quedan por confirmar con el inventario de abajo. |

## Recuperar el esquema parcial

### 1. Inventario de solo lectura

Detené los procesos que apunten a la demo o suspendé su arranque. En una sesión MySQL con el host y el puerto de **Connect**, autenticá con contraseña solicitada por el cliente y con la CA indicada por TiDB. Seleccioná exclusivamente `hogwarts_library_demo`.

```powershell
$tidbHost = Read-Host 'Hostname de TiDB Cloud Connect'
$tidbPort = Read-Host 'Puerto'
$tidbAuditUser = Read-Host 'Usuario autorizado para inspeccionar el esquema'
$tidbCa = Read-Host 'Ruta local al CA PEM indicado por TiDB'
mysql --host=$tidbHost --port=$tidbPort --user=$tidbAuditUser -p --ssl-mode=VERIFY_IDENTITY --ssl-ca=$tidbCa --database=hogwarts_library_demo
```

```sql
SELECT DATABASE() AS esquema, VERSION() AS version, @@foreign_key_checks AS fk_activas;
SHOW SESSION STATUS LIKE 'Ssl_cipher';

SELECT TABLE_NAME, TABLE_TYPE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'hogwarts_library_demo'
ORDER BY TABLE_NAME;

SELECT TRIGGER_NAME FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = 'hogwarts_library_demo';
SELECT ROUTINE_NAME, ROUTINE_TYPE FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'hogwarts_library_demo';

SELECT TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_SCHEMA, REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_SCHEMA IS NOT NULL
  AND (TABLE_SCHEMA = 'hogwarts_library_demo' OR REFERENCED_TABLE_SCHEMA = 'hogwarts_library_demo')
ORDER BY TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME;
```

Si aparecen las tablas esperadas, ejecutá estas consultas. Compará el inventario con las estructuras parciales informadas: `categoria`, `cliente`, `libro`, `DATABASECHANGELOG` y `DATABASECHANGELOGLOCK`. Revisá cualquier objeto adicional antes de seguir.

```sql
SELECT ID, AUTHOR, FILENAME, EXECTYPE, MD5SUM
FROM DATABASECHANGELOG ORDER BY ORDEREXECUTED;
SELECT ID, LOCKED, LOCKGRANTED FROM DATABASECHANGELOGLOCK;
SHOW CREATE TABLE libro;
SHOW INDEX FROM libro;

SELECT 'categoria' AS tabla, COUNT(*) AS filas FROM categoria
UNION ALL SELECT 'cliente', COUNT(*) FROM cliente
UNION ALL SELECT 'libro', COUNT(*) FROM libro;
```

En el estado reportado se esperan solo `001-create-categoria` y `002-create-cliente` ejecutados, el índice y la FK `fk_libro_categoria`, y cero filas en esas tres tablas del dominio. Si hay filas, verificá su origen y cotejá los identificadores con `fictitious-library.sql` en una sesión privada. Ante datos no identificados como descartables, un lock activo, objetos inesperados o dependencias entre esquemas, detené la limpieza y revisá el inventario.

### 2. Elegir cómo reinicializar

La opción que conserva el estado previo es crear una instancia Starter dentro de la cuota gratuita, con gasto en 0, y preparar allí un esquema vacío `hogwarts_library_demo` y un usuario de servicio dedicado. Validá la demo en esa instancia antes de cambiar su JDBC. [Cuotas de TiDB Cloud Starter](https://docs.pingcap.com/tidbcloud/serverless-limitations/).

Si decidís recuperar el esquema en la misma instancia, guardá primero una exportación de su DDL, datos e historial fuera del repositorio, limitada a `hogwarts_library_demo`. Verificá que la exportación corresponda al inventario. Con el servicio detenido, autorizá explícitamente la reinicialización y ejecutá como administrador de ese esquema. Confirmá de nuevo la instancia y el nombre de base antes del DDL. [Exportación de TiDB Cloud Starter](https://docs.pingcap.com/tidbcloud/serverless-export/).

```sql
SELECT DATABASE() AS esquema, VERSION() AS version;
-- Solo tras confirmar el inventario, la exportación y la autorización.
DROP DATABASE `hogwarts_library_demo`;
CREATE DATABASE `hogwarts_library_demo` CHARACTER SET utf8mb4;
```

Confirmá con `SHOW GRANTS` los permisos del usuario de servicio sobre `hogwarts_library_demo.*`, incluido `REFERENCES`, y verificá que el esquema esté vacío. Liquibase creará sus propias tablas de control al migrar el esquema recreado. Conservá el respaldo hasta verificar ambos arranques.

## Validación y reinicio de Spring Boot

Desde la raíz del repositorio, con Java 21 y el esquema vacío, reutilizá los secretos existentes o cargalos en la sesión de PowerShell. Usá el hostname exacto de **Connect** y un origen HTTPS exacto para CORS. Si Java requiere un CA adicional, incorporalo al truststore que usa el proceso, o montalo mediante `MYSQL_CA_FILE` al usar la imagen. [Configuración de variables y CA](DEPLOYMENT_GUIDE.md#variables-de-entorno).

```powershell
$tidbHost = Read-Host 'Hostname de TiDB Cloud Connect'
$tidbPort = Read-Host 'Puerto'
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:DB_URL = "jdbc:mysql://${tidbHost}:${tidbPort}/hogwarts_library_demo?sslMode=VERIFY_IDENTITY"
if (!$env:DB_USERNAME) { $env:DB_USERNAME = Read-Host 'Usuario de servicio completo' }
if (!$env:DB_PASSWORD) { $env:DB_PASSWORD = Read-Host 'Contraseña de servicio' -MaskInput }
if (!$env:JWT_SECRET) { $env:JWT_SECRET = Read-Host 'Secreto JWT de la demo' -MaskInput }
if (!$env:CORS_ALLOWED_ORIGINS) { $env:CORS_ALLOWED_ORIGINS = Read-Host 'Origen HTTPS del frontend' }
$env:PORT = '8080'
.\mvnw.cmd -B spring-boot:run
```

Confirmá en el arranque los perfiles `demo` y `hosted`, los 15 changesets completados, la validación de Hibernate y el inicio de Tomcat. En otra terminal PowerShell 7, comprobá la API sin imprimir el JWT.

```powershell
$api = 'http://127.0.0.1:8080'
(Invoke-RestMethod "$api/healthz").status  # UP
$demo = Invoke-RestMethod "$api/api/auth/demo" -Method Post
$demo.rol  # DEMO
$headers = @{ Authorization = "Bearer $($demo.token)" }
(Invoke-WebRequest "$api/api/libros" -Headers $headers).StatusCode  # 200
(Invoke-WebRequest "$api/api/libros" -SkipHttpErrorCheck).StatusCode  # 401
(Invoke-WebRequest "$api/api/libros" -Method Post -Headers $headers -ContentType 'application/json' -Body '{}' -SkipHttpErrorCheck).StatusCode  # 403
```

En la base, comprobá las FK y que ningún índice implícito duplique un índice de las mismas columnas. La pareja `libro_autor(libro_id)` y `libro_autor(libro_id, autor_id)` es deliberada y tiene firmas distintas.

```sql
SELECT COUNT(*) AS claves_foraneas
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE();

SELECT TABLE_NAME, columnas, COUNT(*) AS indices
FROM (
  SELECT TABLE_NAME, INDEX_NAME,
         GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columnas
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
  GROUP BY TABLE_NAME, INDEX_NAME
) AS firmas
GROUP BY TABLE_NAME, columnas HAVING COUNT(*) > 1;

SELECT COUNT(*) AS cambios FROM DATABASECHANGELOG;
SELECT COUNT(*) AS cargas_demo FROM DATABASECHANGELOG WHERE ID = 'demo-001-fictitious-library';
SELECT COUNT(*) AS ejemplares FROM ejemplar;
SELECT COUNT(*) AS pagos FROM pago;
SELECT ultimo_valor FROM secuencia_codigo WHERE nombre = 'EJEMPLAR';
```

Se esperan ocho FK, cero firmas duplicadas, 15 changesets, una carga demo, seis ejemplares, tres pagos y el contador en 6. En una instancia descartable de validación, comprobá además el rechazo de una referencia inexistente con el usuario de servicio. Ejecutá cada sentencia por separado en la misma sesión. El `INSERT` debe devolver un error de integridad referencial, como `1216` o `1452` según el motor y los privilegios.

```sql
START TRANSACTION;
INSERT INTO libro (isbn, titulo, categoria_id)
VALUES ('DEMO-FK-TEST', 'Prueba de integridad', -1);
ROLLBACK;
```

Detené Spring Boot con Ctrl+C y repetí `.\mvnw.cmd -B spring-boot:run` en la misma sesión. Confirmá `/healthz`, repetí los conteos, y comprobá que Liquibase informe cero changesets nuevos. La guía contiene el orden de publicación después de las comprobaciones. [Guía de despliegue](DEPLOYMENT_GUIDE.md).
