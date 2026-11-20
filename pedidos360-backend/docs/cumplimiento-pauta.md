# Dónde está cada requisito de la EP5

| Requisito | Dónde |
|---|---|
| Backend: varios microservicios Java + Spring Boot | `ms-*`, `bff` |
| Frontend Angular | `frontend/` |
| Compila y responde a pruebas básicas | `./mvnw test` en cada servicio (pruebas unitarias en `src/test`) |
| Integración con BD cloud (entidades, repositorios, propiedades) | `model/` + `repository/` de cada servicio; `application.yml` → `DB_URL`, `DB_USER`, `DB_PASSWORD` (PostgreSQL/RDS; sin variables usa H2) |
| Filtros que validan el JWT del IDaaS | `bff/.../config/SecurityConfig.java` (firma + vigencia) y `JwtAudienceValidator.java` (audiencia), `JWT_ISSUER` / `JWT_AUDIENCE` |
| Frontend con login IDaaS y JWT en las llamadas | `frontend/` (MSAL + header `Authorization: Bearer`) |
| `.gitignore` para subir solo lo necesario | `.gitignore` raíz, `bff/.gitignore`, `.dockerignore` por servicio |
| Productores y consumidores RabbitMQ | `bff/.../messaging/RegistroPedidoPublisher`, `ms-pedidos/.../messaging`, `ms-notificaciones`, `ms-despacho` |
| DLQ y registro en logs | `ConsumidorConAck` (NACK sin reencolar → DLX → DLQ + log) |
| Microservicio administrador de RabbitMQ (colas, exchanges, bindings) | `ms-rabbitmq-admin` |
| Productores y consumidores Kafka | productores: `ms-pedidos`, `ms-despacho`, `ms-productos`; consumidores: `ms-analitica/.../kafka` |
| Microservicio administrador de Kafka (tópicos, configuración) | `ms-kafka-admin` |
| docker-compose clúster RabbitMQ | `infra/rabbitmq/docker-compose.nodo1.yml`, `.nodo2.yml` (AWS) y `docker-compose.cluster-local.yml` (1 máquina) |
| docker-compose Kafka con Zookeeper y brokers | `infra/kafka/docker-compose.yml` |
| docker-compose Kafka UI | `infra/kafka/docker-compose.kafka-ui.yml` |
| Cliente Kafka completo (bootstrap, serializers, groups, propiedades) | `application.yml` de ms-pedidos / ms-despacho / ms-productos / ms-analitica (sección `spring.kafka`) |
| DTO tipados y validación de payload | `ms-analitica/.../dto` + `EventoParser` |
| Manejo de errores (logging, validación, reintentos) | `ms-analitica/.../config/KafkaConfig` (reintentos + DLT) |
| Parámetros de tópicos (retención, cleanup, particiones, réplicas) | `infra/kafka/crear-topicos.sh`, `KafkaConfig` de los productores, `ms-kafka-admin` `application.yml` |
| Consumer groups | `PedidosEventosListener`, `InventarioListener` (`analitica-kpis`, `analitica-inventario`, `analitica-auditoria`) |
