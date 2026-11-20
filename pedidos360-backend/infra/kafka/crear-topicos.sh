#!/usr/bin/env bash
# Crea los tópicos de Pedidos360 (idempotente: --if-not-exists). También se usa desde ms-kafka-admin.
set -euo pipefail
BS="kafka-1:29092,kafka-2:29092,kafka-3:29092"

echo "Esperando a los brokers..."
for i in $(seq 1 30); do
  kafka-broker-api-versions --bootstrap-server "$BS" >/dev/null 2>&1 && break
  sleep 3
done

# Eventos del ciclo de vida de un pedido. Retención 7 días.
kafka-topics --bootstrap-server "$BS" --create --if-not-exists --topic pedidos.eventos \
  --partitions 3 --replication-factor 3 \
  --config retention.ms=604800000 --config cleanup.policy=delete --config min.insync.replicas=2

# Movimientos de stock. Retención 1 día (dato operativo de corta vida).
kafka-topics --bootstrap-server "$BS" --create --if-not-exists --topic inventario.movimientos \
  --partitions 3 --replication-factor 3 \
  --config retention.ms=86400000 --config cleanup.policy=delete --config min.insync.replicas=2

kafka-topics --bootstrap-server "$BS" --describe --topic pedidos.eventos
kafka-topics --bootstrap-server "$BS" --describe --topic inventario.movimientos
