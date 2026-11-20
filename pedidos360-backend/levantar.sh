#!/usr/bin/env bash
# Levanta todo Pedidos360 en orden: Kafka -> RabbitMQ + microservicios -> Kafka UI.
set -euo pipefail
[ -f .env ] || cp .env.example .env
docker compose -f infra/kafka/docker-compose.yml up -d
docker compose up --build -d
docker compose -f infra/kafka/docker-compose.kafka-ui.yml --env-file .env up -d
docker compose ps
echo "Kafka UI: http://localhost:8090  |  RabbitMQ: http://localhost:15672  |  BFF: http://localhost:8080"
