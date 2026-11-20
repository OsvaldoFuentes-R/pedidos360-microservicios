# Pedidos360

Plataforma de gestión de pedidos con arquitectura de **microservicios**, mensajería con **RabbitMQ** (comandos) y
**Kafka** (streaming de eventos), y frontend Angular autenticado con **Azure CIAM**. Curso DSY1107 Cloud Native I (Duoc UC).

## Estructura del repositorio

| Carpeta | Puerto | Responsabilidad |
|---|---|---|
| `frontend/` | 4200 | Angular. Login con Azure CIAM y vista de pedidos |
| `bff/` | 8080 | *Backend for Frontend*: valida el JWT, agrega datos de varios servicios y publica pedidos nuevos en RabbitMQ |
| `ms-productos/` | 8081 | Catálogo y stock (reserva/liberación atómica). **Produce** `inventario.movimientos` (Kafka) |
| `ms-clientes/` | 8082 | Clientes (email único) |
| `ms-pedidos/` | 8083 | **Consume** `pedidos.registro` (RabbitMQ), valida cliente y stock, guarda el pedido, publica notificación y ticket (RabbitMQ) y **produce** `pedidos.eventos` (Kafka) |
| `ms-notificaciones/` | 8084 | **Consume** `pedidos.notificaciones` y guarda/“envía” la notificación |
| `ms-despacho/` | 8085 | **Consume** `pedidos.tickets`, genera el ticket y maneja su estado. **Produce** `pedidos.eventos` (Kafka) |
| `ms-kafka-admin/` | 8086 | Administra Kafka: tópicos, configuración, consumer groups y lag |
| `ms-rabbitmq-admin/` | 8087 | Administra RabbitMQ: crear/eliminar colas, exchanges y bindings |
| `ms-analitica/` | 8088 | **Consume** los dos tópicos Kafka (3 consumer groups): KPIs, stock crítico y auditoría |
| `infra/rabbitmq/` | – | Cluster RabbitMQ de 2 nodos (AWS, o los 2 en una máquina) |
| `infra/kafka/` | – | 3 Zookeeper + 3 brokers + creación de tópicos, y Kafka UI |
| `docs/` | – | Arquitectura, RabbitMQ, Kafka, despliegue y guía de la presentación |

Cada microservicio es un proyecto Spring Boot 3.3 / Java 21 independiente, con su propia base de datos
(H2 en memoria por defecto, PostgreSQL en la nube con variables de entorno), `pom.xml`, `Dockerfile` y pruebas unitarias.

## Flujo

```
Frontend ──JWT──▶ API Manager ──▶ BFF ──(RabbitMQ) pedidos.registro──▶ ms-pedidos ──REST──▶ ms-productos / ms-clientes
                                                                          ├─(RabbitMQ) pedidos.notificaciones──▶ ms-notificaciones
                                                                          ├─(RabbitMQ) pedidos.tickets─────────▶ ms-despacho
                                                                          └─(Kafka) pedidos.eventos ──────┐
                                  ms-despacho ──(Kafka) pedidos.eventos ────────────────────────────────────┤
                                  ms-productos ─(Kafka) inventario.movimientos ─────────────────────────────┼──▶ ms-analitica
Frontend ◀── GET /api/pedidos, /api/analitica/*  (el BFF combina ms-pedidos, ms-despacho y ms-analitica)
```
Detalle en [`docs/arquitectura.md`](docs/arquitectura.md), [`docs/rabbitmq.md`](docs/rabbitmq.md) y [`docs/kafka.md`](docs/kafka.md).

## Ejecutar todo con Docker (≈6 GB de RAM libres)

```bash
cp .env.example .env
./levantar.sh        # Kafka -> RabbitMQ + microservicios -> Kafka UI
```
- RabbitMQ: http://localhost:15672 · Kafka UI: http://localhost:8090 · BFF: http://localhost:8080
- El BFF exige un JWT de Azure CIAM; para probar los microservicios sin token usa directamente los puertos 8081–8088.

## Ejecutar un servicio sin Docker
```bash
cd ms-productos && ./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
./mvnw test                                    # pruebas unitarias
```
Las variables de cada servicio están en su `src/main/resources/application.yml` (`RABBITMQ_HOST`,
`KAFKA_BOOTSTRAP_SERVERS`, `MS_PRODUCTOS_URL`, `DB_URL`, …).

## Base de datos en la nube
Sin variables, cada servicio usa H2 en memoria. Para PostgreSQL (RDS) define por servicio:
```
DB_URL=jdbc:postgresql://<host>:5432/pedidos360?sslmode=require&currentSchema=<esquema>
DB_USER=...   DB_PASSWORD=...
```
con un esquema por servicio (`productos`, `clientes`, `pedidos`, `notificaciones`, `despacho`, `analitica`).
Crea los esquemas antes (`CREATE SCHEMA productos;` …). Las tablas las crea Hibernate (`DB_DDL_AUTO=update`).

## Probar un pedido (sin BFF)
Publica en el exchange `pedidos360.exchange` con routing key `pedido.registro` desde el panel de RabbitMQ:
```json
{"clienteId":1,"items":[{"productoId":1,"cantidad":2}]}
```
Luego revisa `GET :8083/pedidos`, `:8084/notificaciones`, `:8085/tickets`, `:8088/analitica/kpis` y los tópicos en Kafka UI.
Un JSON roto o un producto sin stock termina en `pedidos.registro.dlq`.

## Seguridad
- No se suben `.env`, `.pem` ni contraseñas (ver `.gitignore`). Cambia las contraseñas por defecto (RabbitMQ, Kafka UI).
- Los IDs de tenant/cliente de Azure son identificadores públicos.
