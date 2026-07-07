-- =====================================================================
-- V1: creacion de la tabla reserva (Flyway).
-- El esquema se versiona aqui; Hibernate solo VALIDA (ddl-auto: validate).
-- =====================================================================
CREATE TABLE reserva (
    id           BIGSERIAL       PRIMARY KEY,
    usuario      VARCHAR(100)    NOT NULL,
    origen       VARCHAR(120)    NOT NULL,
    destino      VARCHAR(120)    NOT NULL,
    fecha_viaje  TIMESTAMP       NOT NULL,
    asiento      VARCHAR(10)     NOT NULL,
    precio       NUMERIC(10,2)   NOT NULL CHECK (precio > 0),
    estado       VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE',
    creado_en    TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Indice para el filtro por propietario (RBAC a nivel de datos)
CREATE INDEX idx_reserva_usuario ON reserva (usuario);
