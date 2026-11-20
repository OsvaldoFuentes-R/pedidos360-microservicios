# Kafka (3 Zookeeper + 3 brokers + Kafka UI)

```bash
cp .env.example .env
docker compose up -d          # 3 zookeeper, 3 brokers y kafka-init (crea los tópicos)
docker compose logs kafka-init
```
Kafka UI (`docker compose -f docker-compose.kafka-ui.yml up -d`): http://localhost:8090 → *Topics*: `pedidos.eventos` e `inventario.movimientos`, 3 particiones y 3 réplicas cada uno.

## Por qué esta configuración
- **3 Zookeeper**: quórum de 2 de 3; el ensamble sobrevive a la caída de uno. Elige el controlador y guarda metadatos.
- **3 brokers**: cada partición tiene 3 réplicas (1 líder + 2 seguidoras). `min.insync.replicas=2` + `acks=all` en los
  productores: un mensaje confirmado está en al menos 2 brokers, y se tolera la caída de 1 broker sin perder datos.
- **3 particiones**: hasta 3 consumidores del mismo grupo leen en paralelo (una partición por consumidor).
  La clave del mensaje (`pedidoId`) hace que los eventos de un mismo pedido vayan siempre a la misma partición y
  conserven su orden.
- **Retención/limpieza**: `pedidos.eventos` 7 días, `inventario.movimientos` 1 día, ambos `cleanup.policy=delete`.

## Prueba de tolerancia a fallos
`docker stop pedidos360-kafka-kafka-2-1` → en Kafka UI las particiones cambian de líder y los servicios siguen
publicando y consumiendo. `docker start ...` y el broker vuelve a quedar *in-sync*.

## Conectar los microservicios
- En el mismo Docker network: `KAFKA_BOOTSTRAP_SERVERS=kafka-1:29092,kafka-2:29092,kafka-3:29092`
- Desde fuera: `localhost:9092,localhost:9093,localhost:9094` (o `IP_EC2:9092,...`; abre 9092–9094 en el Security Group).
