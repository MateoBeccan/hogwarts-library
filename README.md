# Hogwarts Library

Sistema full stack de gestión bibliotecaria desarrollado con **Java, Spring Boot, Vue 3, TypeScript y MySQL**.

Permite administrar clientes, categorías, libros, ejemplares físicos, alquileres, devoluciones y pagos.

## Funcionalidades principales

- Autenticación con JWT.
- Roles `ADMIN` y `EMPLEADO`.
- Gestión de clientes, categorías, libros y ejemplares.
- Control de ejemplares disponibles, alquilados y fuera de servicio.
- Creación, finalización y cancelación de alquileres.
- Registro de pagos parciales.
- Cálculo de total pagado y saldo pendiente.
- Prevención de sobrepagos.
- Dashboard con métricas reales.
- Baja lógica y reactivación de recursos.
- Migraciones de base de datos con Liquibase.
- Pruebas unitarias y de integración.

## Tecnologías

### Backend

- Java 21
- Spring Boot 4
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- MySQL 8
- Liquibase
- Maven

### Frontend

- Vue 3

### Testing

- JUnit 5
- Mockito
- H2
- MySQL para pruebas de migración

## Reglas de negocio destacadas

- Solo pueden alquilar clientes activos.
- Solo pueden seleccionarse ejemplares disponibles.
- Al crear un alquiler, los ejemplares pasan a `ALQUILADO`.
- Al finalizar o cancelar, vuelven a `DISPONIBLE`.
- No se permiten pagos mayores al saldo pendiente.
- Un alquiler finalizado puede seguir recibiendo pagos si mantiene deuda.
- Las operaciones críticas utilizan transacciones y locks pesimistas.
- Los códigos de ejemplar se generan automáticamente con formato `HOG-000001`.

## Arquitectura

El backend utiliza una arquitectura por capas:

```text
Controller
   ↓
Service
   ↓
Repository
   ↓
Database

Ejecutar el backend

Crear la base de datos:

CREATE DATABASE hogwarts_library_db;

Configurar las variables de entorno:

$env:DB_USERNAME="usuario"
$env:DB_PASSWORD="contraseña"
$env:JWT_SECRET="clave-jwt-segura"

Ejecutar:

.\mvnw spring-boot:run

El backend estará disponible en:

http://localhost:8080
Ejecutar el frontend
cd frontend
npm install
npm run dev

El frontend estará disponible en:

http://localhost:5173
Tests
.\mvnw test


Versiones
V1.0.0

Primera versión estable del backend con:

seguridad JWT;
gestión de catálogo;
alquileres y devoluciones;
pagos;
reglas de negocio;
pruebas automatizadas;
esquema versionado con Liquibase.
Evolución actual
Frontend completo con Vue 3.
Dashboard con información real.
Flujo completo de alquileres y pagos.
Código automático de ejemplares.
Mejoras de experiencia de usuario.
