# Estado de preparación de Hogwarts Library

**Rama candidata** `feature/v2.2.1-hardening`. La auditoría inicial partió de `f249008`. La corrección de Docker se preparó sobre `b0d64f1`.

## TiDB Cloud Starter

La opción gratuita para la demo combina TiDB Cloud Starter en modo MySQL con límite de gasto en 0, Render Free y Vercel Hobby. El primer arranque remoto informado llegó a Liquibase con TLS y se detuvo en `003-create-libro`, después de que TiDB creara un índice implícito con el nombre de la clave foránea. Las migraciones ahora crean los índices antes de las claves y aceptan los seis checksums históricos exactos de los changesets reordenados.

La verificación local con MySQL 8.0.43 pasó 74 pruebas. El JAR arrancó sobre el historial MySQL previo, conservó los 14 registros y ejecutó cero changesets. En MySQL demo vacío, el primer arranque aplicó 15 y el siguiente cero. Se comprobaron las ocho claves foráneas, la ausencia de índices duplicados, Hibernate `validate`, TLS, la carga ficticia, el JWT demo, el bloqueo de escrituras y `/healthz`.

La validación posterior informada sobre TiDB Cloud Starter aplicó 15 changesets y confirmó el arranque de Hibernate, los datos ficticios, `/healthz` en `UP`, la emisión del JWT, las lecturas REST y el rechazo HTTP 403 de escrituras demo. Queda pendiente repetir el smoke test desde el contenedor desplegado en Render. [Informe de compatibilidad con TiDB](TIDB_COMPATIBILITY_REPORT.md).

## Corrección del build Docker en Render

El SHA-256 fijado corresponde al ZIP de Maven 3.9.16. Ambos binarios oficiales se cotejaron contra sus SHA-512 publicados. Para el digest base fijado, el inventario SPDX de `linux/amd64` incluye `wget` y `coreutils` y carece de `unzip`. Sin `unzip`, `mvnw` selecciona el TAR.GZ, cuyo SHA-256 difiere, y aborta antes de invocar Maven. `Dockerfile` instala `unzip` solo en `build` y conserva la validación SHA-256. [Hashes y procedimiento de despliegue](DEPLOYMENT_GUIDE.md#integridad-de-maven-wrapper-en-docker), [imagen oficial](https://hub.docker.com/layers/library/eclipse-temurin/21-jdk-jammy/images/sha256-4e38bcd64a552586921cdd011831cabc0d56c4f7c950aa460c0b9e03237871ed?tab=layers).

| Validación | Resultado |
| --- | --- |
| Maven Wrapper POSIX con cachés vacías, usando los binarios oficiales ya descargados | Sin `unzip`, seleccionó TAR.GZ y reprodujo el error SHA-256. Con `unzip`, validó el ZIP y ejecutó Maven 3.9.16 sobre Java 21. |
| Scripts y contexto Docker | `sh -n` pasó para `mvnw` y `docker/entrypoint.sh`. Se comprobó LF en ambos y que `.dockerignore` permite las entradas de compilación. `mvnw.cmd` usa el ZIP y verifica su hash antes de extraerlo. |
| Suite Java 21 y MySQL 8.0.43 local aislado | `mvnw.cmd -o -B -ntp verify` pasó 74 pruebas, con 0 fallos, errores u omisiones. |
| Precarga de dependencias | `dependency:go-offline` pasó con una caché nueva en Windows. La JVM usó el almacén de confianza de Windows para acceder a Maven Central. |
| Empaquetado del Dockerfile reproducido localmente | `compile jar:jar spring-boot:repackage` produjo `library-1.0.0.jar` con `JarLauncher`, la aplicación y Connector/J en `BOOT-INF`. |
| Configuración de la etapa final | Revisión estática del JRE 21 fijado por digest, UID/GID 10001, `MaxRAMPercentage=50`, `PORT`, perfil `demo` con `hosted`, Connector/J con `VERIFY_IDENTITY`, CA opcional y `/healthz` como ruta indicada para el health check de Render. |
| Imagen y despliegue | Docker no está disponible en este host. Quedan pendientes el build y arranque de la imagen en Linux, la verificación de memoria con 512 MB y el smoke test con TiDB desde Render. |

## Auditoría inicial

El árbol de trabajo estaba limpio. `feature/v2-evolution` y ambas ramas remotas de evolución y hardening apuntaban al mismo commit `f249008`, que agrega códigos automáticos de ejemplar, autores normalizados y las migraciones 002 y 003. Los commits `7451561` y `d1ab23f` ordenan los archivos generados del frontend. `develop` apuntaba a la V1 en `389929c`; el `main` local estaba en `df73d9d` y su remoto en `c83474d`. La rama de respaldo precede a los cambios de evolución. Esta comparación usa las referencias del checkout al inicio del trabajo.

El proyecto contiene una API Java 21/Spring Boot 4.1.0 organizada en controladores, servicios, repositorios y mappers; Spring Security 7, JJWT 0.13, JPA/Hibernate, MySQL con Connector/J y Liquibase. Maven Wrapper fija Maven 3.9.16. El frontend en `frontend` usa Vue 3, TypeScript, Vite, Vue Router history, Pinia y Axios, con lockfile de npm. La suite usa JUnit 5, Mockito, MockMvc, H2 y una prueba de arranque sobre MySQL.

| Hallazgo de la auditoría | Cambio aplicado |
| --- | --- |
| El JDBC apuntaba a MySQL local. La configuración principal habilitaba SQL en logs. | Configuración común con Hibernate en `validate` y perfiles `dev`, `prod`, `demo` y `hosted`. TLS verificado y pool pequeño para hosting. |
| El inicializador de desarrollo contenía contraseñas fijas de `ADMIN` y `EMPLEADO`. | Bootstrap de dev optativo por variables. Una cuenta local existente requiere rotación si conserva una contraseña de las semillas anteriores. |
| JWT tenía expiración fija y una cuenta desactivada podía conservar un JWT válido hasta el vencimiento. | Validación de la longitud de la clave al arrancar, expiración por configuración y comprobación de que la cuenta esté habilitada. Las respuestas de login llevan `no-store`. |
| La cadena permitía el prefijo de autenticación completo y usaba una regla autenticada genérica. Los roles normales permiten escrituras. | Autorización explícita. El perfil demo emite JWT para una identidad en memoria y permite solo lecturas concretas. Los perfiles normales mantienen el acceso de `ADMIN` y `EMPLEADO` sobre los controladores existentes. |
| No había configuración CORS de hosting. Axios usaba el proxy Vite y guardaba el token en `localStorage`. | Orígenes exactos, API HTTPS validada al construir, sesión en `sessionStorage`, manejo de 401, 403, 429 y fallos de conexión. |
| El README describía V1. Docker y Vercel carecían de archivos de configuración en esta rama. | Configuración Vite/Vercel para rutas directas, imagen Java 21 con usuario sin privilegios y CA adicional en runtime, health check y guías. |
| No había catálogo, clientes ni operaciones ficticias aisladas. | Changelog demo sobre un esquema dedicado y vacío. Reutiliza las migraciones comunes 001–003. |
| Un fallo no controlado escribía la excepción completa en los logs. | La respuesta sigue siendo genérica. El log de aplicación conserva método, clase y ubicación del error para diagnóstico. |

Se revisaron el README, pom, npm y lockfile, archivos de configuración, perfiles de prueba, referencias de entorno, changelogs, seguridad y JWT, controladores REST, Axios, navegación, pruebas y posibles credenciales. Al inicio no había Dockerfile, Compose, YAML de configuración Spring ni archivos `.env` versionados. Los valores de prueba fijos permanecen en código de test. No se cambiaron las versiones de dependencias durante esta preparación.

## Acceso, datos y operación

Un visitante entra sin credenciales mediante `POST /api/auth/demo`. El token firmado dura 30 minutos por defecto y da acceso a listas y detalles de libros, autores, categorías, ejemplares, clientes, alquileres y pagos ficticios. Las solicitudes directas de escritura, gestión administrativa y documentación técnica están denegadas para la demo. La interfaz oculta formularios y acciones de gestión y permite volver a un detalle tras renovar la sesión. La cuenta `EMPLEADO` conserva sus acciones operativas en desarrollo y producción, de modo que se eligió un permiso `DEMO` específico.

La carga usa el modelo vigente. Incluye 3 autores, 3 categorías, 3 libros, 6 ejemplares, 3 clientes, 4 alquileres y 3 pagos. Usa identificadores ficticios, correos con dominio `.invalid`, importes de ejemplo y relaciones coherentes. Las devoluciones se representan con la fecha de devolución del alquiler. La demo comparte lecturas sobre un conjunto estático para que varias visitas puedan explorarla sin agotar ejemplares, crear registros ni alterar la experiencia de otras sesiones. Las fechas se establecen al inicializar y envejecen con el tiempo. Una actualización futura de los casos puede usar un changeset exclusivo de demo o una nueva instancia demo verificada.

El endpoint público `GET /healthz` verifica disponibilidad de Spring y acceso a la base con respuesta mínima. La aplicación no incorporó Actuator. La API aplica CORS HTTPS con allowlist, además de usar secretos de entorno, comprobación de cuenta habilitada y respuestas sin stack traces. La entrada de Docker permite instalar un CA PEM sin incorporar certificados privados al build.

## Validaciones de la preparación inicial

| Validación | Resultado |
| --- | --- |
| Compilación, empaquetado y suite con `mvnw.cmd -o -B verify` | 73 pruebas, 0 fallos, 0 errores, 0 omitidas. Incluye 14 casos de acceso/CORS demo, pruebas de login y JWT, roles, reglas de alquiler/pago y migración real sobre MySQL. |
| MySQL limpio, esquema demo y TLS | MySQL 8.0.43 local aislado. El perfil demo aplicó 15 changeSets y arrancó con Hibernate `validate` verificando la identidad TLS del servidor. Se comprobaron lecturas reales del catálogo, ejemplares, clientes, alquileres y pagos. |
| Smoke REST, segunda carga y desarrollo | La API respondió 200, 401, 403 y 404 según la operación y aplicó la lista CORS. Tras el reinicio siguió habiendo 6 ejemplares, 3 pagos y una sola ejecución de la semilla. El perfil demo rechazó un esquema incorrecto antes de migrar tablas del dominio. El perfil dev permitió autenticar al administrador, crear catálogo y ejemplares, alquilar, devolver, cancelar y registrar pagos parciales. Rechazó el alquiler de un ejemplar ocupado, el sobrepago y el pago de un alquiler cancelado. |
| Frontend y navegador | Instalación limpia con npm ci, TypeScript y build Vite exitosos. El build rechazó una URL HTTP local. Chrome verificó el reintento tras un timeout simulado, el acceso demo, las búsquedas, el filtro por estado, la recarga y el detalle de un pago. La ruta de alta redirigió al dashboard. |
| Docker y proveedores externos | El JAR ejecutable fue generado y probado localmente y el script de entrada pasó la comprobación de sintaxis POSIX. El host carece de Docker, por lo que quedan pendientes el build y arranque del contenedor y la medición de RAM con 512 MB. También quedan pendientes los checks sobre Render o Railway, MySQL administrado, Vercel y el arranque en frío real. |
| Lint y pruebas frontend | `frontend/package.json` declara `dev`, `build` y `preview`. TypeScript se ejecuta en el build. El repositorio carece de comandos de lint y de suite automatizada frontend. |

## Archivos

| Área | Creados | Modificados |
| --- | --- | --- |
| Documentación y contenedor | `DEPLOYMENT_GUIDE.md`, `DEMO_READINESS_REPORT.md`, `Dockerfile`, `.dockerignore`, `docker/entrypoint.sh`, `.env.example` | `README.md`, `.gitignore`, `.gitattributes`, `.mvn/wrapper/maven-wrapper.properties` |
| Configuración de API | `src/main/resources/application-dev.properties`, `application-hosted.properties`, `application-demo.properties` | `src/main/resources/application.properties` |
| Backend | `controller/DemoAuthController.java`, `controller/HealthController.java`, `security/DemoUserDetailsService.java` | `config/DataInitializer.java`, `config/SecurityConfig.java`, `controller/AuthController.java`, `exception/GlobalExceptionHandler.java`, `security/CustomUserDetailsService.java`, `security/JwtAuthenticationFilter.java`, `security/JwtService.java` |
| Liquibase | `db/changelog/db.changelog-demo.yaml`, `db/changelog/demo/fictitious-library.sql` | La corrección TiDB reordena el DDL de las migraciones 001 y 003 y conserva sus checksums históricos. |
| Pruebas | `security/DemoSecurityIntegrationTest.java` | `LibraryApplicationTests.java`, `src/test/resources/application-migration.properties` |
| Frontend | `frontend/vercel.json`, `frontend/.env.example` | `vite.config.ts`, `src/api/auth.api.ts`, `src/api/axios.ts`, `src/types/auth.ts`, `src/stores/auth.store.ts`, `src/router/index.ts`, `src/layouts/AppLayout.vue`, `src/styles/main.css` |
| Vistas frontend | — | `LoginView`, `DashboardView`, `AlquileresView`, `AlquilerDetailView`, `AutoresView`, `AutorDetailView`, `CategoriasView`, `ClientesView`, `ClienteDetailView`, `EjemplaresView`, `EjemplarDetailView`, `LibrosView` y `LibroDetailView` |

## Riesgos y publicación

1. Validá la imagen con Docker antes de publicar. Comprobá el consumo de memoria del proceso y el arranque con el límite del plan elegido.
2. Revisá la versión de `feature/v2.2.1-hardening` y usá el esquema `hogwarts_library_demo` ya validado. Confirmá los datos de conexión y TLS desde Render. [Guía de despliegue](DEPLOYMENT_GUIDE.md#volver-a-desplegar-el-fix-en-render-free).
3. Desplegá la API Docker con perfil `demo`, el CA correspondiente y `/healthz` como health check. Luego publicá Vite en Vercel apuntando al backend HTTPS y autorizá el origen exacto de Vercel en la API.
4. Revisá el primer arranque, una recarga de rutas y los rechazos de escrituras del rol demo sobre la URL pública. Controlá memoria, conexiones y latencia. Render Free puede tardar alrededor de un minuto en reactivar el servicio. El frontend señala la espera y permite reintentar. La aplicación no incorpora un limitador distribuido y el tráfico abusivo puede consumir capacidad de la API o de MySQL. Un límite perimetral puede cubrir ese riesgo cuando se configure el dominio definitivo.
5. Verificá la URL pública antes de enlazarla desde el portfolio. Las ilustraciones PNG existentes suman aproximadamente 23 MB en el build completo y pueden penalizar la primera visita con una red lenta. Se conserva el material visual existente.

La corrección de Docker está en el workspace. La verificación del build y del arranque en Render queda pendiente de publicar el cambio en la rama enlazada.

Mensaje de commit sugerido

```
fix(docker): ensure Maven Wrapper distribution integrity on Linux
```
