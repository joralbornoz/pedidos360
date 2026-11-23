#!/bin/bash
# Crea los tópicos de Pedidos360 con sus configuraciones.
# Es idempotente, se puede correr las veces que sea.
# Lo ejecuta el contenedor kafka-init del docker-compose.

set -e

BOOTSTRAP="${BOOTSTRAP:-kafka1:29092,kafka2:29092,kafka3:29092}"

echo ">> Esperando que los 3 brokers estén disponibles..."
until [ "$(kafka-broker-api-versions --bootstrap-server "$BOOTSTRAP" 2>/dev/null | grep -c '(id:')" -ge 3 ]; do
  echo "   aún no están los 3 brokers, reintentando en 5s"
  sleep 5
done
echo ">> Brokers listos"

# Tópico 1: eventos del ciclo de vida de los pedidos (key = order-{id})
#  - delete + 7 días: es un historial de eventos, interesa poder re-leer la última semana
kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
  --topic pedidos360.order-events \
  --partitions 3 --replication-factor 3 \
  --config cleanup.policy=delete \
  --config retention.ms=604800000 \
  --config min.insync.replicas=2 \
  --config segment.ms=86400000

# Tópico 2: cambios del catálogo (key = product-{id})
#  - compact: solo importa el último estado de cada producto, Kafka limpia los eventos viejos
kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
  --topic pedidos360.catalog-events \
  --partitions 3 --replication-factor 3 \
  --config cleanup.policy=compact \
  --config min.insync.replicas=2 \
  --config min.compaction.lag.ms=60000 \
  --config segment.ms=3600000 \
  --config delete.retention.ms=86400000

# Dead Letter Topics: mensajes que fallaron después de los reintentos (14 días para revisarlos)
for dlt in pedidos360.order-events.DLT pedidos360.catalog-events.DLT; do
  kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$dlt" \
    --partitions 3 --replication-factor 3 \
    --config cleanup.policy=delete \
    --config retention.ms=1209600000 \
    --config min.insync.replicas=2
done

echo ">> Tópicos actuales:"
kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic 'pedidos360.*'