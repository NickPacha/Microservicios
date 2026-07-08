# Sistema Transporte — Monorepo de microservicios

![CI](https://github.com/NickPacha/Microservicios/actions/workflows/ci.yml/badge.svg)

Un solo repositorio, microservicios independientes: cada uno tiene su propio `pom.xml`, puerto, base de datos y JVM. El pom raíz solo agrega los módulos para compilar todo junto.

## Estructura

```
Microservicios/
├── pom.xml          → agregador (compila los tres módulos)
├── arrancar.bat     → inicia los tres microservicios (doble clic)
├── detener.bat      → detiene lo que corra en 8080/8083/8084
├── ms-gateway/      → API Gateway (8080) - valida JWT en el perímetro y enruta
├── ms-reserva/      → reservas (8083) - JWT, RBAC, H2/PostgreSQL, circuit breaker
└── ms-pagos/        → pagos (8084) - procesa los pagos que envía ms-reserva
```

## Arrancar todo

Doble clic en **`arrancar.bat`** (o desde terminal). Abre dos ventanas, una por microservicio. Cuando ambas muestren `Tomcat started`:

| URL | Qué es |
|---|---|
| http://localhost:8083/ | Portal: registro de usuarios, login, reservas y suite de pruebas |
| http://localhost:8083/swagger-ui.html | Swagger UI de ms-reserva |
| http://localhost:8084/api/pagos | Pagos procesados por ms-pagos (ahora exige JWT: usa Swagger/Postman con token, o el portal) |
| http://localhost:8080/api/reservas | Lo mismo pero entrando por el API Gateway (valida el JWT en el perímetro) |

Usuarios semilla: `fernando/fernando123` [USER] · `admin/admin123` [ADMIN]

## Arrancar con Docker (recomendado)

Con Docker Desktop instalado, un solo comando levanta todo el stack (los 3 microservicios + PostgreSQL real con migraciones Flyway):

```bash
docker compose up --build
```

La primera vez tarda varios minutos (compila las imágenes). Después: mismas URLs de siempre, con una diferencia importante — **las reservas sobreviven a los reinicios** (PostgreSQL con volumen, no H2 en memoria). Para detener: `docker compose down` (o `docker compose down -v` para borrar también los datos).

## Compilar todo de una vez

```bash
cd ms-reserva && mvnw clean install    # o desde la raíz con Maven instalado: mvn clean install
```

## Cómo se integran

`ms-reserva` llama a `ms-pagos` vía OpenFeign al crear una reserva:

- **ms-pagos corriendo** → pago procesado → reserva `CONFIRMADA`
- **ms-pagos apagado** → circuit breaker (Resilience4j) → reserva `PENDIENTE` (la petición no falla)

`ms-pagos` es un **Resource Server**: valida el JWT igual que `ms-reserva`, y `ms-reserva` reenvía el token del usuario en su llamada Feign (identidad extremo a extremo). Los pagos se persisten con `UNIQUE(reserva_id)`, de modo que la **idempotencia sobrevive a reinicios**.

En el perfil `local`, Feign resuelve `ms-pagos` → `localhost:8084` por registro estático (sin Eureka). En producción la resolución la hace Eureka y la identidad Keycloak.

Más detalle: `ms-reserva/PRUEBAS-WEB.md` y la colección Postman en `ms-reserva/postman/`.
