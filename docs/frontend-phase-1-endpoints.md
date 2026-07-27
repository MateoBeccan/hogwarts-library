# Frontend Fase 1: endpoints revisados

Backend inspeccionado sin modificaciones:

- `AuthController`: expone `POST /api/auth/login`.
- `LoginRequest`: body JSON `{ "username": string, "password": string }`.
- `LoginResponse`: respuesta exitosa `{ "token": string, "type": "Bearer", "username": string, "rol": "ADMIN" | "EMPLEADO" }`.
- `SecurityConfig`: permite `/api/auth/**` sin JWT. El resto de `/api/clientes/**`, `/api/categorias/**`, `/api/libros/**`, `/api/ejemplares/**`, `/api/alquileres/**` y `/api/pagos/**` requiere rol `ADMIN` o `EMPLEADO`; algunas acciones administrativas requieren `ADMIN`.
- `ApiError`: errores con `{ timestamp, status, error, message, path, validationErrors }`.

Uso en Fase 1:

- Login: `POST /api/auth/login`.
- Requests autenticados futuros: header `Authorization: Bearer {token}` mediante interceptor Axios.
- Guard de rutas: usuarios sin token van a `/login`; usuarios autenticados que abren `/login` van a `/dashboard`.
