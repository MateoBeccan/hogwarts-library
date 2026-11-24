# Desplegar Hogwarts Library

La demo pública usa el frontend Vue en Vercel, una API Spring Boot en Render y un MySQL administrado en Aiven. El navegador usa HTTPS para comunicarse con la API y Java verifica tanto el certificado como el nombre del servidor MySQL. La entrada pública es el botón **Acceder a la demo**. La sesión permite explorar la biblioteca ficticia con un JWT de solo lectura.

## Proveedores y costos

| Componente | Opción recomendada | Límites y alternativas |
| --- | --- | --- |
| Frontend | Vercel, con raíz `frontend` | Hobby está orientado a uso personal no comercial. Revisá las condiciones y los límites de transferencia del portfolio. |
| API | Render Web Service, runtime Docker | Free tiene 512 MB y se suspende tras 15 minutos de inactividad. El próximo acceso puede tardar alrededor de un minuto. Starter se anuncia desde USD 7/mes con 512 MB y sin suspensión por inactividad. Si el RSS supera el margen disponible, elegí una instancia con más memoria. |
| MySQL | Aiven para MySQL, plan Developer | Aiven retiró el plan gratuito para nuevas bases MySQL. Developer se anuncia a USD 5/mes con 8 GB y un solo nodo, con copias de seguridad y sin SLA de alta disponibilidad. |
| Alternativa | Railway para la API y Aiven para MySQL, o Railway para ambos | El plan Hobby incluye USD 5 de uso contra la suscripción mensual y cobra el excedente. La plantilla MySQL de Railway usa un volumen persistente. Configurá copias, actualizaciones, TLS con identidad verificada y red privada. La plantilla requiere más operación que Aiven. |

Precios y planes consultados el 24 de noviembre de 2026. Confirmá el costo final, regiones, salida de red y condiciones antes de contratar. Presupuestá desde USD 5/mes con API Free o desde USD 12/mes con API Starter y MySQL Developer, sujeto a los planes elegidos y cargos adicionales. Mantené API y MySQL geográficamente cerca. Usá las políticas de acceso al MySQL que admita el plan y autorizá la salida de red del backend cuando el proveedor permita restringirla. [Vercel Hobby](https://vercel.com/docs/plans/hobby), [Render Free](https://render.com/docs/free), [precios de Render](https://render.com/pricing), [Aiven MySQL](https://aiven.io/free-mysql-database-retired), [precios de Railway](https://docs.railway.com/pricing/plans), [MySQL en Railway](https://docs.railway.com/databases/mysql).

## Requisitos y perfiles

Usá Java 21, el Maven Wrapper del repositorio, MySQL 8 compatible con Connector/J y Node.js 22.12 o una versión admitida superior. Para empaquetar el contenedor localmente necesitás Docker. Tomá como base la rama `feature/v2.2.1-hardening` después de revisar e incorporar los cambios del workspace.

| Perfil Spring | Base | Acceso |
| --- | --- | --- |
| `dev` (predeterminado) | MySQL local, Liquibase, Hibernate `validate` | Credenciales de usuarios locales. El bootstrap optativo crea cuentas ausentes. |
| `test` | H2 en memoria | Pruebas de servicios, MVC y seguridad. |
| `migration` (pruebas) | MySQL aislado `hogwarts_library_migration_test` | Verifica que Liquibase e Hibernate arranquen sobre MySQL. |
| `prod` | MySQL dedicado, conexión verificada por TLS | Cuentas gestionadas por el operador, con roles `ADMIN` y `EMPLEADO`. |
| `demo` | MySQL dedicado al esquema `hogwarts_library_demo` | Inicio de sesión público sin contraseña y consultas explícitas con rol `DEMO`. |

Seleccioná un perfil principal por entorno. `demo` y `prod` activan también `hosted`, que concentra la configuración común. Spring no carga los archivos `.env` automáticamente. Usá las variables del proveedor o exportalas en el shell. Vite sí lee sus archivos de entorno. Los ejemplos del repositorio usan dominios reservados y dejan los secretos vacíos.

## Variables de entorno

| Variable | Ámbito | Valor o efecto |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | API | `demo` para el portfolio, `prod` para operación real o `dev` para desarrollo. |
| `DB_URL` | API | JDBC de MySQL. Obligatoria en hosting. `dev` tiene un valor local predeterminado. |
| `DB_USERNAME`, `DB_PASSWORD` | API | Usuario de servicio y contraseña con alcance sobre el esquema dedicado. |
| `JWT_SECRET` | API | Secreto aleatorio independiente por entorno, al menos 32 bytes. |
| `JWT_EXPIRATION_MS` | API, opcional | Demo 1.800.000 ms, resto 3.600.000 ms. |
| `CORS_ALLOWED_ORIGINS` | API | Orígenes exactos separados por coma. En hosting, solo HTTPS y sin rutas ni barra final. |
| `PORT` | API, opcional | Puerto interno del proveedor, por defecto 8080. |
| `MYSQL_CA_FILE` | Contenedor, opcional | Ruta a un CA en PEM montado en tiempo de ejecución. Necesaria para una CA ausente del truststore de Java. |
| `DEV_ADMIN_PASSWORD`, `DEV_EMPLEADO_PASSWORD` | API, solo dev | Crean `admin` o `empleado` si faltan. Deben tener al menos 12 caracteres y un máximo de 72 bytes. |
| `MIGRATION_DB_URL` | Maven, opcional | JDBC de una base MySQL de pruebas aislada. |
| `VITE_API_URL` | Build frontend | URL pública HTTPS de la API, terminada en `/api`. |
| `VITE_DEMO_MODE` | Build frontend, opcional | `true` para mostrar el acceso público. |
| `VITE_DEV_API_TARGET` | Servidor Vite, opcional | Destino del proxy local. Por defecto `http://localhost:8080`. |

Generá una clave con `openssl rand -hex 32` y guardala en el gestor de secretos del proveedor. La rotación invalida los JWT ya emitidos, por lo que los visitantes deberán ingresar otra vez. Las variables `VITE_*` llegan al navegador. Reservalas para direcciones y opciones de interfaz.

## 1. Preparar MySQL

1. Creá un servicio MySQL dedicado a la demo y registrá host, puerto, versión y certificado de CA. En Railway, usá el host privado y un certificado válido para ese nombre si API y base comparten proyecto. Instalá la CA que firma el certificado.
2. Creá el esquema vacío con una cuenta de administración de MySQL.

   ```sql
   CREATE DATABASE hogwarts_library_demo CHARACTER SET utf8mb4;
   ```

3. Creá un usuario de servicio exclusivo con alcance en `hogwarts_library_demo.*`. El proceso de arranque necesita crear y alterar las tablas e índices, agregar claves foráneas, ejecutar las migraciones DML, y mantener las tablas de control y bloqueo de Liquibase. Un grant del proveedor limitado a este esquema simplifica el primer arranque. La cuenta de administración de la instancia queda fuera del servicio.
4. Configurá `DB_URL` con el formato `jdbc:mysql://HOST:PUERTO/hogwarts_library_demo?sslMode=VERIFY_IDENTITY` y entregá usuario y contraseña por separado. Conservá el hostname del certificado. Si la CA es privada, instalala en el runtime como se indica abajo. Hikari usa hasta 5 conexiones por instancia, 1 ociosa mínima y esperas acotadas. Reservá conexiones para el arranque simultáneo durante un despliegue y para mantenimiento. [Propiedades TLS de Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html).
5. Activá o confirmá las copias de seguridad del proveedor y registrá el procedimiento de restauración. MySQL proporciona persistencia. Las réplicas de la API pueden compartir la misma base de demostración.

Al arrancar con `demo`, el changelog valida que el motor sea MySQL y que el esquema se llame `hogwarts_library_demo`. Ejecuta las migraciones 001 a 003 y, solo si las tablas de negocio están vacías, registra `demo-001-fictitious-library`. El bloqueo de Liquibase coordina arranques concurrentes y el historial impide repetir la carga registrada. Hibernate valida el esquema en hosting. Los datos incluyen seis ejemplares con tres estados, cuatro alquileres, una devolución, una cancelación, un anticipo y un alquiler saldado. Las fechas se calculan durante la carga inicial y quedan fijas.

## 2. Validar antes de publicar

Ejemplo para PowerShell con MySQL local. Creá `hogwarts_library_db` y `hogwarts_library_migration_test` vacías con credenciales de desarrollo. El perfil `migration` aplica cambios al esquema apuntado por `MIGRATION_DB_URL`.

```powershell
$env:DB_USERNAME = 'usuario_local'
$env:DB_PASSWORD = Read-Host 'Contraseña MySQL' -MaskInput
$env:JWT_SECRET = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
.\mvnw.cmd -B verify
```

Para iniciar localmente, configurá opcionalmente `DEV_ADMIN_PASSWORD` y `DEV_EMPLEADO_PASSWORD` desde tu shell y ejecutá `.\mvnw.cmd spring-boot:run` con el perfil `dev`. Los cambios de esas variables se aplican a las cuentas nuevas. Si una instalación local previa conserva una contraseña de las antiguas semillas, rotala de forma controlada en su base local. Para el frontend local, ejecutá `npm ci` y `npm run dev` en `frontend` con `VITE_API_URL` sin definir. El proxy Vite apunta al backend local. Para explorar la demo local vía ese proxy, arrancá la API con perfil `demo`, MySQL dedicado y TLS, y configurá `VITE_DEMO_MODE=true` para Vite. En hosting, CORS requiere orígenes HTTPS.

Docker usa Java 21 en dos etapas con imágenes Eclipse Temurin fijadas por digest, Maven Wrapper con checksum verificado, un JRE y UID/GID 10001. El contexto admite únicamente fuentes y archivos de construcción. La construcción del contenedor empaqueta el JAR. La compilación, la suite y las migraciones de prueba se validan con `mvn verify` antes de construir el contenedor.

```sh
docker build -t hogwarts-library:demo-candidate .
docker run --rm --name hogwarts-library-demo --env-file .env.demo \
  -v "/ruta/absoluta/ca.pem:/etc/secrets/mysql-ca.pem:ro" \
  -e MYSQL_CA_FILE=/etc/secrets/mysql-ca.pem -p 127.0.0.1:8080:8080 hogwarts-library:demo-candidate
curl --fail http://127.0.0.1:8080/healthz
```

Creá `.env.demo` localmente a partir de `.env.example`, con un MySQL demo alcanzable desde el contenedor y `PORT=8080`. El host de `DB_URL` debe coincidir con el certificado incluso al ejecutar Docker localmente. Para una CA pública ya reconocida por Java, omití el volumen y `MYSQL_CA_FILE`. La entrada del contenedor importa la CA adicional a una copia temporal del truststore.

## 3. Publicar la API en Render

1. Tras autorizar la publicación y subir la versión revisada, creá un Web Service conectado al repositorio. Elegí **Docker**, la rama aprobada y raíz del repositorio con Dockerfile `./Dockerfile`. Dejá que Render asigne `PORT`. Usá un despliegue manual hasta completar el primer smoke test.
2. Agregá las variables de la API. Seleccioná `SPRING_PROFILES_ACTIVE=demo`, el JDBC de la base ficticia y una clave JWT exclusiva. En **Health Check Path** usá `/healthz`.
3. Si el servicio requiere CA propia, agregá el PEM en **Secret Files** con nombre `mysql-ca.pem`, y `MYSQL_CA_FILE=/etc/secrets/mysql-ca.pem`. Configurá `CORS_ALLOWED_ORIGINS` con el dominio de frontend reservado. Si aún falta el dominio, usá temporalmente un origen HTTPS bajo `.invalid` y reemplazalo por el dominio real al finalizar el paso de Vercel.
4. Desplegá. Revisá los logs de conexión TLS, changelogs aplicados y arranque de Tomcat. Comprobá desde fuera `GET /healthz` con HTTP 200 y `{"status":"UP"}`. Una base inaccesible o una app que no acepta tráfico devuelve 503 y `{"status":"DOWN"}`. El endpoint solo devuelve estado. [Web Services](https://render.com/docs/web-services), [health checks](https://render.com/docs/health-checks), [variables y archivos secretos](https://render.com/docs/configure-environment-variables).

En Railway, conectá el mismo Dockerfile como API, definí las mismas variables, generá el dominio HTTPS, configurá `/healthz` y ajustá los recursos de ejecución. Si montás la CA desde un archivo o configurás el truststore por otro mecanismo, verificá TLS desde el runtime real. [Spring Boot en Railway](https://docs.railway.com/guides/spring-boot), [health checks en Railway](https://docs.railway.com/deployments/healthchecks).

## 4. Publicar Vue en Vercel

1. Importá el repositorio como proyecto Vite con **Root Directory** igual a `frontend` y la rama aprobada como Production Branch. Seleccioná una versión de Node compatible.
2. En el alcance **Production**, configurá `VITE_API_URL` con el hostname HTTPS real de Render o Railway más `/api`, y `VITE_DEMO_MODE=true`. El build usa `npm ci`, `npm run build` y publica `dist`. `vercel.json` reescribe las rutas SPA a `/index.html` para admitir URLs directas y recargas.
3. Desplegá y agregá el origen HTTPS exacto de Vercel a `CORS_ALLOWED_ORIGINS` en la API. Si usás un dominio propio, agregá explícitamente solo las variantes que servirán el frontend. Para Preview, elegí una URL estable y autorizala de forma explícita si necesita usar la API. Un cambio en variables `VITE_*` requiere un nuevo build. [Vite en Vercel](https://vercel.com/docs/frameworks/frontend/vite).

## 5. Probar el entorno publicado

- Desde una sesión privada, abrí el frontend, entrá con **Acceder a la demo**, buscá un libro, filtrá ejemplares por estado y abrí alquileres con devolución, cancelación y pago parcial. Recargá una ruta interna mientras la sesión esté vigente.
- En la API, `GET /healthz` debe devolver 200 y un cuerpo mínimo. `POST /api/auth/demo` devuelve un JWT de rol `DEMO` con `Cache-Control: no-store`. Usá el token solo en memoria al hacer la comprobación. `GET /api/libros` devuelve 401 sin token y 200 con token; las mutaciones y recursos administrativos devuelven 403 con token demo.
- Revisá el preflight CORS desde el origen desplegado y desde un origen ajeno. El autorizado debe recibir `Access-Control-Allow-Origin` con el valor exacto. Comprobá también la primera visita después de una suspensión y la opción **Reintentar acceso**.
- Verificá en el proveedor conexión MySQL cifrada con validación de identidad, consumo de memoria, uso de conexiones y latencia. Confirmá que la base contiene solo el conjunto ficticio y observá el arranque tras un reinicio.
- Cuando la URL de Vercel haya pasado estos controles, incorporala al estudio de caso del portfolio.

## Operación y rollback

La demo comparte un conjunto de datos fijo. Las sesiones públicas pueden leerlo en paralelo y las escrituras quedan bloqueadas por la API. No se necesita un proceso de limpieza por visitante. Para cambiar los ejemplos, agregá un changeset exclusivo de demo y revisá los datos resultantes en una base descartable. Si necesitás recuperar la demo, inicializá una instancia demo nueva con el mismo nombre de esquema y credenciales exclusivas, validala y cambiá el JDBC. Conservá la instancia anterior hasta terminar la verificación. Configurá alertas de disponibilidad, latencia y consumo. Si el tráfico lo exige, aplicá límites de solicitudes en una capa perimetral que cubra la API pública, con especial atención a `POST /api/auth/demo`. La emisión del JWT demo usa una identidad en memoria y una firma HMAC, sin una consulta MySQL ni verificación BCrypt; las consultas autenticadas sí consumen recursos de la base.

Para actualizar, ejecutá `mvn verify` y el build Vite, respaldá MySQL, probá el cambio en un esquema descartable y subí ambas partes desde el commit revisado. Desplegá primero un backend compatible con la versión anterior del frontend. Luego actualizá Vercel y repetí el smoke test. Para revertir, restaurá las versiones verificadas desde **Rollback** en Render y **Instant Rollback** en Vercel. Un rollback de aplicación deja intacto el esquema que Liquibase ya migró. Si hay incompatibilidad, usá una migración correctiva o restaurá un respaldo en una instancia nueva y validala antes de cambiar la conexión. [Rollback en Render](https://render.com/docs/rollbacks), [rollback en Vercel](https://vercel.com/docs/instant-rollback).

| Problema | Comprobación |
| --- | --- |
| API sin arrancar | Revisá `PORT`, credenciales, valor de `JWT_SECRET`, límites de RAM y logs de Liquibase. |
| Error TLS en MySQL | Confirmá host y puerto del proveedor, CA vigente, ruta de `MYSQL_CA_FILE` y nombre en el certificado. |
| Error al inicializar demo | Verificá el nombre del esquema y que la primera carga use tablas sin datos de negocio. Revisá los changeSets y el bloqueo de Liquibase antes de reintentar. |
| Bloqueo CORS | Compará el `Origin` exacto de la solicitud con `CORS_ALLOWED_ORIGINS` y reiniciá la API tras corregirlo. |
| Acceso lento o 502/503 | Esperá el arranque, revisá `/healthz`, disponibilidad del MySQL y arranques en frío. El acceso demo permite esperar hasta 90 segundos y reintentar. |
| Una ruta interna responde 404 | Revisá que Vercel use `frontend` como raíz y aplique `frontend/vercel.json`. |
| JWT vencido o revocado por rotación | Volvé a ingresar. La interfaz conserva la ruta interna cuando corresponde. |
