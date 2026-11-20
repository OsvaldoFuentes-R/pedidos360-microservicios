# Cluster RabbitMQ de 2 nodos (AWS EC2)

Versión **3.13.x** con colas espejadas (*classic mirrored queues*). Con 2 nodos no se usan quorum queues ni 4.x
(Khepri): necesitan mayoría y con 2 nodos un nodo caído deja el cluster sin quórum.

## Requisitos de red (Security Group `sg-rabbitmq`)
| Puerto | Origen | Para qué |
|---|---|---|
| 22 | EC2 Instance Connect (`18.206.107.24/29`) o tu IP | administrar |
| 5672 | el SG del BFF / microservicios | AMQP |
| 15672 | tu IP | panel web |
| 4369, 25672 | el **mismo** SG (autorreferencia) | comunicación entre nodos |

## Pasos
1. En **cada** EC2: instalar Docker y copiar este directorio. `cp .env.example .env` y completar (misma cookie en ambos).
2. Nodo 1: `docker compose -f docker-compose.nodo1.yml up -d`
3. Nodo 2: `docker compose -f docker-compose.nodo2.yml up -d`
4. Unir el nodo 2 al nodo 1 (se ejecuta en la EC2 del nodo 2):
   ```bash
   docker exec rabbitmq-nodo2 rabbitmqctl stop_app
   docker exec rabbitmq-nodo2 rabbitmqctl reset
   docker exec rabbitmq-nodo2 rabbitmqctl join_cluster rabbit@rabbitmq-nodo1
   docker exec rabbitmq-nodo2 rabbitmqctl start_app
   ```
5. Política de espejado (en cualquier nodo):
   ```bash
   docker exec rabbitmq-nodo1 rabbitmqctl set_policy ha-pedidos "^pedidos\." \
     '{"ha-mode":"all","ha-sync-mode":"automatic"}' --apply-to queues
   ```
6. Verificar: `docker exec rabbitmq-nodo1 rabbitmqctl cluster_status` (debe listar los 2 nodos en *Running Nodes*).

## Que los servicios usen los dos nodos
Cada microservicio con RabbitMQ (`bff`, `ms-pedidos`, `ms-notificaciones`, `ms-despacho`) se inicia con:
```
RABBITMQ_ADDRESSES=IP_NODO1:5672,IP_NODO2:5672
RABBITMQ_USER=...   RABBITMQ_PASSWORD=...
```
Si un nodo cae, el cliente se reconecta al otro en 1–3 s. No vuelve solo al nodo recuperado hasta reiniciar.
(`ms-pedidos`, `ms-notificaciones` y `ms-despacho` leen `RABBITMQ_HOST/PORT`; si usas el cluster agrega
`spring.rabbitmq.addresses` con la variable de entorno `SPRING_RABBITMQ_ADDRESSES`, que tiene prioridad.)

## Prueba de caída (para la demostración)
1. Con el sistema funcionando, `docker stop rabbitmq-nodo1` en la EC2 1.
2. Envía un pedido: sigue procesándose por el nodo 2.
3. `docker start rabbitmq-nodo1` y revisa `cluster_status`.
