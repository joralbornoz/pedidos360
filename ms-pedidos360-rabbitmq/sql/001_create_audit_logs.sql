-- Migración: tabla audit_logs para ms-pedidos360-rabbitmq
-- Ejecutar en Supabase SQL Editor

CREATE TABLE IF NOT EXISTS audit_logs (
    id         BIGSERIAL    PRIMARY KEY,
    servicio   VARCHAR(100) NOT NULL,
    recurso    VARCHAR(100) NOT NULL,
    accion     VARCHAR(100) NOT NULL,
    detalle    TEXT,
    timestamp  TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Índices para las queries del AuditController
CREATE INDEX IF NOT EXISTS idx_audit_logs_servicio
    ON audit_logs (servicio, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_recurso_accion
    ON audit_logs (recurso, accion, timestamp DESC);

-- Comentarios de columnas
COMMENT ON TABLE  audit_logs            IS 'Registro de auditoría de eventos de Pedidos360 vía RabbitMQ';
COMMENT ON COLUMN audit_logs.servicio   IS 'Microservicio origen del evento (ej: ms-pedidos360-orders)';
COMMENT ON COLUMN audit_logs.recurso    IS 'Entidad afectada (ej: order, product)';
COMMENT ON COLUMN audit_logs.accion     IS 'Acción ejecutada (ej: created, updated, status_changed)';
COMMENT ON COLUMN audit_logs.detalle    IS 'Descripción libre del evento';
COMMENT ON COLUMN audit_logs.timestamp  IS 'Timestamp del evento (tal como lo publica el productor)';
COMMENT ON COLUMN audit_logs.created_at IS 'Timestamp de persistencia en DB';
