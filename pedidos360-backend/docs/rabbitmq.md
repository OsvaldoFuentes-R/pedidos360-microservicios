# RabbitMQ

## Topología
| Elemento | Nombre | Tipo / detalle |
|---|---|---|
| Exchange principal | `pedidos360.exchange` | direct; `alternate-exchange` = `pedidos360.unroutable` |
| Exchange alternativo | `pedidos360.unroutable` | fanout → cola `pedidos.noruteados` |
| Dead Letter Exchange | `pedidos360.dlx` | topic |
| Colas | `pedidos.registro`, `pedidos.notificaciones`, `pedidos.tickets` | cada una con su DLQ `pedidos.*.dlq` |
| Routing keys | `pedido.registro`, `pedido.notificacion`, `pedido.ticket` | la key no es el nombre de la cola |

| Productor | Consumidor |
|---|---|
| `bff` → `pedido.registro` | `ms-pedidos` |
| `ms-pedidos` → `pedido.notificacion` | `ms-notificaciones` |
| `ms-pedidos` → `pedido.ticket` | `ms-despacho` |

Los nombres se definen solo en `application.yml` (sección `pedidos360.rabbitmq`) y deben ser **idénticos** en los
4 servicios que declaran la topología; si difieren, RabbitMQ responde `PRECONDITION_FAILED`.

## ACK manual y DLQ
Los consumidores usan `ackMode = MANUAL`: éxito → `basicAck`; error (JSON inválido, datos faltantes, sin stock)
→ `basicNack(requeue=false)` y el mensaje pasa por el DLX a la DLQ. Un mensaje con routing key inexistente
va al exchange alternativo y queda en `pedidos.noruteados`.

## Para la presentación
1. Enviar `{"clienteId":1,"items":[{"productoId":1,"cantidad":2}]}` a `pedido.registro` → aparecen pedido, notificación y ticket.
2. Enviar `esto no es json` → log “FALLIDO … NACK” y mensaje en `pedidos.registro.dlq`.
3. Enviar con routing key `pedido.inexistente` → mensaje en `pedidos.noruteados`.
4. Apagar un nodo del cluster (ver `infra/rabbitmq/README.md`) y repetir el paso 1.

## Administración (ms-rabbitmq-admin, puerto 8087)
Crea y elimina por AMQP (`AmqpAdmin`) y lista con la API HTTP de administración (puerto 15672).

| Método y ruta | Acción |
|---|---|
| `GET /rabbit/colas` · `GET /rabbit/colas/{nombre}` | listar · estado (mensajes, consumidores) |
| `POST /rabbit/colas` | crear cola (con o sin DLQ) |
| `DELETE /rabbit/colas/{nombre}` · `DELETE /rabbit/colas/{nombre}/mensajes` | eliminar · vaciar |
| `GET/POST /rabbit/exchanges` · `DELETE /rabbit/exchanges/{nombre}` | exchanges direct, topic, fanout, headers |
| `GET/POST/DELETE /rabbit/bindings` | enlazar exchange → cola/exchange con routing key. Ej: `{"exchange":"pedidos360.exchange","destino":"pedidos.auditoria","tipoDestino":"QUEUE","routingKey":"pedido.auditoria"}` |

Los elementos de la arquitectura (`pedidos360.*`, colas `pedidos.*`) están protegidos: borrarlos exige `?forzar=true`.

## Cluster de 2 nodos
- AWS: `infra/rabbitmq/docker-compose.nodo1.yml` y `.nodo2.yml` (un nodo por EC2).
- Una sola máquina (demo): `docker compose -f infra/rabbitmq/docker-compose.cluster-local.yml up -d`; el servicio
  `rabbit-cluster` une el nodo 2 al nodo 1 y crea la política de espejado.
