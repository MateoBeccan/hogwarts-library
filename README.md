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
- TypeScript
- Vite
- Pinia
- Vue Router
- Axios

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
```

## Instalación

### Requisitos previos

Antes de ejecutar el proyecto, asegurarse de tener instalado:

- Java 21
- MySQL 8
- Node.js
- npm
- Git

### 1. Clonar el repositorio

```bash
git clone https://github.com/MateoBeccan/hogwarts-library.git
cd hogwarts-library
```

### 2. Crear la base de datos

Ingresar a MySQL y ejecutar:

```sql
CREATE DATABASE hogwarts_library_db;
```

### 3. Configurar variables de entorno

En PowerShell:

```powershell
$env:DB_USERNAME="usuario"
$env:DB_PASSWORD="contraseña"
$env:JWT_SECRET="clave-jwt-segura"
```

Reemplazar los valores por las credenciales reales del entorno local.

### 4. Ejecutar el backend

Desde la raíz del proyecto:

```powershell
.\mvnw spring-boot:run
```

El backend estará disponible en:

```text
http://localhost:8080
```

Liquibase aplicará automáticamente las migraciones pendientes al iniciar la aplicación.

### 5. Instalar dependencias del frontend

Abrir otra terminal y ejecutar:

```bash
cd frontend
npm install
```

### 6. Ejecutar el frontend

```bash
npm run dev
```

El frontend estará disponible en:

```text
http://localhost:5173
```

## Ejecutar pruebas

Desde la raíz del proyecto:

```powershell
.\mvnw test
```

Estado actual:

```text
Tests run: 45
Failures: 0
Errors: 0
BUILD SUCCESS
```

## Versiones

### V1.0.0

Primera versión estable del backend con:

- Seguridad JWT.
- Gestión de catálogo.
- Alquileres y devoluciones.
- Pagos.
- Reglas de negocio.
- Pruebas automatizadas.
- Esquema versionado con Liquibase.

### Evolución actual

- Frontend completo con Vue 3.
- Dashboard con información real.
- Flujo completo de alquileres y pagos.
- Código automático de ejemplares.
- Mejoras de experiencia de usuario.

## Autor

**Mateo Beccan**

Proyecto personal desarrollado para aplicar conocimientos de Java, Spring Boot, bases de datos, seguridad, testing y desarrollo full stack.
