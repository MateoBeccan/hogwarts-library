# Hogwarts Library

API REST desarrollada con Java y Spring Boot para gestionar el alquiler de libros físicos de una biblioteca.

El proyecto fue diseñado como una aplicación backend con reglas de negocio reales, control de inventario por ejemplar, pagos, autenticación JWT, autorización por roles, concurrencia y versionado de base de datos mediante Liquibase.

## Versión actual

**V1.0.0**

La V1 representa la primera versión funcional y estable del sistema.

## Tecnologías

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Hibernate ORM
- Spring Security
- JWT
- MySQL 8
- Liquibase
- Maven
- JUnit 5
- Mockito
- H2 para tests

## Arquitectura

El proyecto utiliza una arquitectura por capas:

```text
controller
   ↓
service
   ↓
repository
   ↓
database
```

Además se utilizan:

```text
domain
dto
mapper
exception
config
security
```

Los controladores trabajan con DTOs y la lógica de negocio se encuentra centralizada en la capa de servicios.

## Modelo de dominio

Las entidades principales son:

- Usuario
- Cliente
- Categoria
- Libro
- Ejemplar
- Alquiler
- DetalleAlquiler
- Pago

### Libro y Ejemplar

`Libro` representa la información bibliográfica de una obra.

`Ejemplar` representa una copia física concreta de ese libro.

Un libro puede tener múltiples ejemplares.

Estados posibles de un ejemplar:

```text
DISPONIBLE
ALQUILADO
FUERA_SERVICIO
```

## Alquileres

Un alquiler pertenece a un cliente y contiene uno o más ejemplares físicos.

Estados:

```text
ACTIVO
FINALIZADO
CANCELADO
```

Reglas principales:

- El cliente debe estar activo.
- El libro asociado al ejemplar debe estar activo.
- Solo pueden alquilarse ejemplares `DISPONIBLE`.
- No pueden repetirse ejemplares dentro de un mismo alquiler.
- Existe una cantidad máxima configurable de ejemplares.
- Existe una duración máxima configurable del alquiler.
- El precio diario se guarda como snapshot en `DetalleAlquiler`.
- El total del alquiler se calcula al momento de crearlo.
- Al crear el alquiler, los ejemplares pasan a `ALQUILADO`.
- Al finalizar o cancelar el alquiler, vuelven a `DISPONIBLE`.
- Un alquiler finalizado no puede volver a modificarse.

Las operaciones críticas utilizan transacciones y locks pesimistas para reducir problemas de concurrencia.

## Pagos

Un alquiler puede tener múltiples pagos.

Métodos disponibles:

```text
EFECTIVO
TARJETA
TRANSFERENCIA
```

Reglas:

- El monto debe ser mayor a cero.
- No se permiten sobrepagos.
- No se permiten pagos sobre alquileres cancelados.
- Un alquiler finalizado puede continuar recibiendo pagos si todavía posee saldo pendiente.

## Seguridad

La API utiliza autenticación mediante JWT.

Roles disponibles:

```text
ADMIN
EMPLEADO
```

### ADMIN

Posee acceso completo a las operaciones administrativas.

Entre otras operaciones puede:

- desactivar entidades;
- reactivar clientes y libros;
- administrar recursos protegidos.

### EMPLEADO

Puede realizar las operaciones habituales de biblioteca, pero no las operaciones administrativas restringidas.

Los clientes no son usuarios autenticados del sistema.

## Manejo de errores

La API utiliza un manejador global mediante `@RestControllerAdvice`.

Ejemplo de estructura:

```json
{
  "timestamp": "2026-07-27T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción del error",
  "path": "/api/recurso",
  "validationErrors": null
}
```

Se contemplan, entre otros:

- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict
- 415 Unsupported Media Type
- 500 Internal Server Error

Los errores internos son registrados en logs sin exponer detalles técnicos al cliente.

## Base de datos y Liquibase

La base de datos utilizada es MySQL.

Desde la V1, el esquema se administra mediante Liquibase.

```text
src/main/resources/db/changelog/
├── db.changelog-master.yaml
└── changes/
    └── 001-baseline-v1.yaml
```

`001-baseline-v1.yaml` representa el esquema inicial estable de la V1 y no debe modificarse.

Las modificaciones futuras del modelo deberán agregarse mediante nuevos changeSets.

Ejemplo:

```text
001-baseline-v1.yaml
002-nueva-funcionalidad.yaml
003-otra-migracion.yaml
```

Hibernate está configurado con:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

por lo tanto Hibernate valida el esquema pero no lo modifica.

## Configuración

La aplicación utiliza variables de entorno para información sensible.

Variables necesarias:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

Ejemplo de configuración:

```properties
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

app.jwt.secret=${JWT_SECRET}
```

No deben almacenarse credenciales reales dentro del repositorio.

## Tests

La V1 posee pruebas unitarias y de integración sobre:

- reglas de alquiler;
- pagos;
- locks pesimistas;
- JWT;
- carga de usuarios;
- autenticación;
- autorización por roles;
- manejo del contexto Spring;
- creación y validación del esquema con Liquibase.

Estado de la V1:

```text
Tests: 40
Failures: 0
Errors: 0
BUILD SUCCESS
```

Los tests normales utilizan una base H2 aislada.

También existe un test de migración que comprueba:

```text
Base MySQL
    ↓
Liquibase
    ↓
Baseline V1
    ↓
Hibernate validate
    ↓
Spring Boot
```

## Ejecutar el proyecto

### 1. Configurar variables de entorno

PowerShell:

```powershell
$env:DB_USERNAME="usuario"
$env:DB_PASSWORD="password"
$env:JWT_SECRET="clave-jwt-segura"
```

### 2. Crear la base de datos

```sql
CREATE DATABASE hogwarts_library_db;
```

### 3. Ejecutar

```powershell
.\mvnw spring-boot:run
```

Liquibase aplicará automáticamente las migraciones pendientes.

## Ejecutar tests

```powershell
.\mvnw test
```

## Evolución del proyecto

La V1 se mantiene intencionalmente limitada para permitir que el proyecto evolucione mediante mantenimiento real.

Las futuras versiones podrán incorporar nuevas reglas y funcionalidades mediante cambios incrementales de código, modelo de dominio y migraciones Liquibase.

## Autor

**Mateo Beccan**

Proyecto desarrollado como práctica de backend profesional con Java, Spring Boot, SQL, seguridad y diseño de reglas de negocio.