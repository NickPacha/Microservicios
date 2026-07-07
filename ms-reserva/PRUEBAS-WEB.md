# Pruebas en la web — ms-reserva

Una sola página en **http://localhost:8083/** con todo integrado: registro de usuarios, login y gestión de reservas. Sin Postman, sin PostgreSQL, sin Keycloak, sin Eureka.

## Arrancar en modo local

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

(o en IntelliJ: Active profiles = `local`)

## La página (http://localhost:8083/)

1. **Usuarios**: registra usuarios con rol USER o ADMIN y ve la lista. Semilla: `fernando/fernando123` [USER] y `admin/admin123` [ADMIN].
2. **Iniciar sesión**: usuario + contraseña → el portal obtiene el JWT y lo usa en todas las llamadas.
3. **Crear reserva** / **Operaciones por ID** / **Listado**: CRUD completo (ADMIN ve todas las reservas, USER solo las suyas).
4. **Suite de pruebas automáticas**: 16 pruebas end-to-end (registro, duplicados, login, token falsificado, validación, CRUD, RBAC, fallback del Circuit Breaker).

También queda **/swagger-ui.html** (botón Authorize con el token del login).

## Cómo funciona (solo perfil `local`)

- `DevAuthController` (`@Profile("local")`): usuarios en memoria con BCrypt; `POST /dev/usuarios` (registro), `GET/DELETE /dev/usuarios`, `POST /dev/login` (emite el JWT con los roles del usuario).
- `LocalJwtConfig`: firma/valida los JWT con clave simétrica HS256 de desarrollo — reemplaza a Keycloak solo en local.
- H2 en memoria en lugar de PostgreSQL; Eureka y Config Server desactivados.
- En producción (perfil por defecto) nada de esto existe: `/dev/**` responde 404 y los tokens se validan contra Keycloak (`issuer-uri`/`jwk-set-uri`). Los usuarios reales se gestionan en Keycloak.

## Verificación ejecutada (Java 17 + Maven, perfil local)

Registro 201, duplicado 409, password corta 400, login incorrecto 401, login USER/ADMIN OK, API sin token 401, creación 201 con propietario tomado del JWT y estado PENDIENTE (fallback del Circuit Breaker sin ms-pagos), DELETE como USER 403, confirmación ADMIN 200 → CONFIRMADA, DELETE ADMIN 204, consola y Swagger 200. **14/14 ✔**

> Nota: los usuarios y reservas del modo local viven en memoria: se reinician al reiniciar la app. La clave HS256 es solo para desarrollo.
