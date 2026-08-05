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
