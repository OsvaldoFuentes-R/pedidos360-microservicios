# Kafka en Pedidos360

Kafka **complementa** a RabbitMQ, no lo reemplaza:

| | RabbitMQ | Kafka |
|---|---|---|
| Para qué | **Comandos**: "registra este pedido", "genera este ticket". Cada mensaje lo procesa un consumidor y se borra. | **Hechos**: "el pedido 7 fue registrado", "bajó el stock de la Laptop". Quedan guardados (retención) y varios servicios los releen de forma independiente. |
| Flujo | BFF → ms-pedidos → ms-notificaciones / ms-despacho | ms-pedidos, ms-productos y ms-despacho → ms-analitica |
| Tolerancia a fallos | Cluster de 2 nodos, colas espejadas, DLQ | 3 brokers, 3 réplicas, DLT |

Si Kafka se cae, los pedidos siguen funcionando (los productores no bloquean y solo registran el error). Si RabbitMQ
se cae, el BFF responde 503. Las integraciones previas con RabbitMQ no cambiaron.

## Arquitectura (infra/kafka)
- **3 Zookeeper** (`zookeeper-1..3`): ensamble con quórum de 2 de 3. Guardan los metadatos del cluster y eligen el
  controlador. Tolera la caída de 1.
- **3 brokers** (`kafka-1..3`): almacenan las particiones. Con 3 réplicas por partición, cada broker guarda una copia.
- **kafka-init**: contenedor de un solo uso que crea los 2 tópicos (idempotente).
- **Kafka UI** (`docker-compose.kafka-ui.yml`): administración visual de brokers, tópicos, mensajes y consumer groups.

## Tópicos (2 tópicos · 3 particiones · 3 réplicas)
| Tópico | Eventos (`tipo`) | Productor | Clave | Retención | cleanup.policy |
|---|---|---|---|---|---|
| `pedidos.eventos` | `PEDIDO_REGISTRADO`, `TICKET_ESTADO_CAMBIADO` | ms-pedidos, ms-despacho | `pedidoId` | 7 días | delete |
| `inventario.movimientos` | `RESERVA`, `LIBERACION` | ms-productos | `productoId` | 1 día | delete |

**Por qué 3 particiones:** hasta 3 instancias del mismo consumer group leen en paralelo (una partición por instancia).
**Por qué 3 réplicas + `min.insync.replicas=2` + `acks=all`:** un mensaje confirmado ya está en al menos 2 brokers;
se tolera la caída de 1 broker sin perder datos ni dejar de publicar.
**Por qué la clave es el id:** todos los eventos de un mismo pedido (o producto) caen en la misma partición y se
leen en orden: primero `PEDIDO_REGISTRADO`, luego `TICKET_ESTADO_CAMBIADO` a `EN_RUTA` y a `ENTREGADO`.
**Retención:** los pedidos son datos de negocio (7 días para reprocesar/auditar); los movimientos de stock son
operativos (1 día). Ambos con `delete`; para un "último estado por clave" se usaría `compact`.

## Diseño de los eventos (DTO)
```json
// pedidos.eventos  (ms-pedidos)
{"eventId":"uuid","tipo":"PEDIDO_REGISTRADO","pedidoId":7,"clienteId":1,"clienteNombre":"Camila",
 "total":1500.00,"fecha":"2026-11-10T10:00:00Z",
 "items":[{"productoId":1,"nombreProducto":"Laptop","cantidad":2,"precioUnitario":750.00}]}

// pedidos.eventos  (ms-despacho)
{"eventId":"uuid","tipo":"TICKET_ESTADO_CAMBIADO","pedidoId":7,"codigoTicket":"TCK-000007",
 "estadoAnterior":"GENERADO","estadoNuevo":"EN_RUTA","fecha":"2026-11-10T10:05:00Z"}

// inventario.movimientos  (ms-productos)
{"eventId":"uuid","tipo":"RESERVA","productoId":1,"nombreProducto":"Laptop","cantidad":2,
 "stockResultante":8,"fecha":"2026-11-10T10:00:00Z"}
```
Cada evento trae `eventId` (idempotencia: el consumidor ignora repetidos), `tipo` (qué pasó), la clave de negocio y
los datos que necesita el consumidor para no tener que consultar a otro servicio.

## Productores (ms-pedidos, ms-despacho, ms-productos)
`KafkaTemplate` con `JsonSerializer`, `acks=all`, `enable.idempotence=true`, `retries=3`. Publican **después** de
guardar en la BD y nunca lanzan excepciones al negocio: si Kafka no responde (`max.block.ms=3s`) se registra el error.

## Consumidores (ms-analitica)
`@KafkaListener` con **tres consumer groups**:

| Grupo | Tópico | Qué hace |
|---|---|---|
| `analitica-kpis` | `pedidos.eventos` | ingresos, ticket promedio, unidades y productos más vendidos |
| `analitica-inventario` | `inventario.movimientos` | último stock de cada producto y stock crítico |
| `analitica-auditoria` | ambos | línea de tiempo con partición y offset de cada evento |

Cada grupo recibe **todos** los mensajes (publicación/suscripción). Dentro de un grupo, si se levantan varias
instancias del servicio (`docker compose up --scale ms-analitica=3`), las 3 particiones se reparten entre ellas.
`concurrency=3` crea un hilo consumidor por partición. `enable-auto-commit=false` + `ack-mode=record`: el offset se
confirma solo después de procesar el mensaje. La lógica de negocio está en los `Service`; los `Listener` solo reciben.

## Manejo de errores
1. **Mensaje inválido** (no es JSON, faltan campos): `EventoInvalidoException`, **no se reintenta** y va directo a la DLT.
2. **Error temporal** (BD caída): se reintenta 2 veces cada 1 s (`FixedBackOff`).
3. Agotados los reintentos, el mensaje se publica en `<tópico>.dlt` (`pedidos.eventos.dlt`,
   `inventario.movimientos.dlt`, misma partición) y el consumidor **sigue con el siguiente**: un mensaje defectuoso no
   bloquea el flujo.
4. Idempotencia por `eventId` (y por tópico/partición/offset en la auditoría).

## Administración (ms-kafka-admin, puerto 8086)
Usa `AdminClient`. Al iniciar crea los tópicos que falten.

| Método y ruta | Acción |
|---|---|
| `GET /kafka/cluster` | brokers, controlador y cantidad de tópicos |
| `GET /kafka/topicos` · `GET /kafka/topicos/{nombre}` | listar · detalle (particiones, líder, réplicas, ISR, configuración) |
| `POST /kafka/topicos` | crear: `{"nombre":"pedidos.auditoria","particiones":3,"replicas":3,"configuracion":{"retention.ms":"86400000"}}` |
| `PUT /kafka/topicos/{nombre}/configuracion` | modificar retención, cleanup.policy, etc. |
| `PUT /kafka/topicos/{nombre}/particiones` | aumentar particiones (Kafka no permite reducirlas) |
| `DELETE /kafka/topicos/{nombre}` | eliminar (los tópicos del sistema están protegidos: requieren `?forzar=true`) |
| `GET /kafka/grupos` · `GET /kafka/grupos/{grupo}` | consumer groups, miembros, offsets y **lag** por partición |
| `DELETE /kafka/grupos/{grupo}` | eliminar un grupo sin miembros |

## Probar
```bash
# 1) Crear un pedido: en el panel de RabbitMQ (http://localhost:15672) publica en el exchange pedidos360.exchange,
#    routing key pedido.registro, el mensaje {"clienteId":1,"items":[{"productoId":1,"cantidad":2}]}
#    (o POST /api/pedidos del BFF con un token válido). Flujo: RabbitMQ -> ms-pedidos -> Kafka -> ms-analitica.
# 2) Avanzar el ticket (genera TICKET_ESTADO_CAMBIADO): PUT localhost:8085/tickets/1/estado  {"estado":"EN_RUTA"}
curl localhost:8088/analitica/kpis
curl localhost:8088/analitica/top-productos
curl "localhost:8088/analitica/stock-critico?umbral=10"
curl "localhost:8088/analitica/auditoria?limite=10"
curl localhost:8086/kafka/grupos/analitica-kpis        # lag = 0 si está al día
```
