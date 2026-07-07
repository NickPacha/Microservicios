# Sistema Transporte — Monorepo de microservicios

Un solo repositorio, microservicios independientes: cada uno tiene su propio `pom.xml`, puerto, base de datos y JVM. El pom raíz solo agrega los módulos para compilar todo junto.

## Estructura

```
Microservicios/
├── pom.xml          → agregador (compila los dos módulos)
├── arrancar.bat     → inicia ambos microservicios (doble clic)
├── detener.bat      → detiene lo que corra en 8083/8084
├── ms-reserva/      → reservas (8083) - JWT, RBAC, H2/PostgreSQL, circuit breaker
└── ms-pagos/        → pagos (8084) - procesa los pagos que envía ms-reserva
```

## Arrancar todo

Doble clic en **`arrancar.bat`** (o desde terminal). Abre dos ventanas, una por microservicio. Cuando ambas muestren `Tomcat started`:

| URL | Qué es |
|---|---|
| http://localhost:8083/ | Portal: registro de usuarios, login, reservas y suite de pruebas |
| http://localhost:8083/swagger-ui.html | Swagger UI de ms-reserva |
| http://localhost:8084/api/pagos | Pagos procesados por ms-pagos |

Usuarios semilla: `fernando/fernando123` [USER] · `admin/admin123` [ADMIN]

## Compilar todo de una vez

```bash
cd ms-reserva && mvnw clean install    # o desde la raíz con Maven instalado: mvn clean install
```

## Cómo se integran

`ms-reserva` llama a `ms-pagos` vía OpenFeign al crear una reserva:

- **ms-pagos corriendo** → pago procesado → reserva `CONFIRMADA`
- **ms-pagos apagado** → circuit breaker (Resilience4j) → reserva `PENDIENTE` (la petición no falla)

En el perfil `local`, Feign resuelve `ms-pagos` → `localhost:8084` por registro estático (sin Eureka). En producción la resolución la hace Eureka y la identidad Keycloak.

Más detalle: `ms-reserva/PRUEBAS-WEB.md` y la colección Postman en `ms-reserva/postman/`.
