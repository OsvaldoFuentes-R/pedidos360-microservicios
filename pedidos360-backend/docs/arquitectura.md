# Arquitectura

## Por qué microservicios
Cada servicio tiene una sola responsabilidad, su propia base de datos y se despliega por separado. Si
`ms-notificaciones` se cae, los pedidos siguen registrándose: los mensajes esperan en la cola.

## Comunicación
- **Síncrona (REST)** solo donde se necesita una respuesta inmediata: `ms-pedidos` → `ms-productos` / `ms-clientes`
  (con timeouts de 2 s de conexión y 5 s de lectura) y BFF → servicios para consultas.
- **Asíncrona (RabbitMQ)** para registrar pedidos y repartir los comandos a notificaciones y despacho.
- **Streaming (Kafka)** para publicar hechos (`pedidos.eventos`, `inventario.movimientos`) que consume `ms-analitica`; ver [`kafka.md`](kafka.md).
- **Administración**: `ms-rabbitmq-admin` (colas, exchanges, bindings) y `ms-kafka-admin` (tópicos, configuración, consumer groups).

## Decisiones de diseño
- **BFF**: el frontend habla con un solo punto, con el formato que ya usa (`id`, `producto`, `estado`).
  El estado visible sale del ticket de despacho; si `ms-despacho` no responde, se muestra “Procesando”.
- **POST /api/pedidos responde 202**: el pedido queda en cola; la creación real es asíncrona.
- **Stock atómico**: `UPDATE ... WHERE stock >= cantidad` evita vender de más con pedidos simultáneos.
- **Compensación**: si falla la reserva del segundo producto, `ms-pedidos` libera lo ya reservado.
- **Idempotencia**: notificaciones y tickets ignoran eventos repetidos del mismo pedido.

## Limitaciones conocidas
- Sin patrón *outbox*: si RabbitMQ o Kafka caen justo después de guardar el pedido, los eventos de notificación/ticket/analítica
  pueden perderse (se registra en el log).
- H2 en memoria (por defecto): los datos se pierden al reiniciar. Con `DB_URL` cada servicio usa PostgreSQL/RDS.
- Consistencia eventual entre servicios.
- 9 JVM + Kafka requieren bastante memoria: Kafka (3 Zookeeper + 3 brokers) ≈ 3 GB y los servicios ≈ 2 GB; los nodos RabbitMQ van en sus propias EC2.
