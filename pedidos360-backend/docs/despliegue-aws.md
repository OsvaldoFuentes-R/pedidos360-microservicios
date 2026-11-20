# Despliegue en AWS

1. **RabbitMQ**: 2 EC2 pequeñas con el cluster de `infra/rabbitmq/` (Security Group `sg-rabbitmq`).
2. **Kafka**: una EC2 de al menos 8 GB (t3.large) con `infra/kafka/docker-compose.yml` y `KAFKA_PUBLIC_HOST=<IP>` en `.env`
   (abrir 9092–9094 solo hacia el Security Group de los microservicios). Kafka UI (`docker-compose.kafka-ui.yml`, puerto 8090)
   solo desde tu IP y con usuario y contraseña propios.
3. **Base de datos**: una instancia RDS PostgreSQL con un esquema por servicio; `DB_URL`, `DB_USER`, `DB_PASSWORD` como variables.
4. **Microservicios + BFF**: una EC2 más grande (≥ 4 GB RAM) con Docker, o varias pequeñas.
   Copiar el repo y crear `.env`; usar un compose que apunte a los nodos RabbitMQ:
   `SPRING_RABBITMQ_ADDRESSES=IP_NODO1:5672,IP_NODO2:5672`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`.
5. **BFF público** (para API Gateway): abrir el puerto 8080 en el Security Group **del BFF**, no en `sg-rabbitmq`.
   Los puertos 8081–8088 no se exponen a Internet. Los administradores (8086, 8087) son internos: acceso solo por túnel SSH.
6. **API Gateway (HTTP API)**: integración HTTP hacia `http://IP_PUBLICA_BFF:8080/api/{proxy}`. Configurar CORS en el
   gateway (origen `http://localhost:4200`, headers `authorization`, `content-type`).
7. Frontend: `frontend/src/environments/environment.ts` → `apiUrl` apunta a la URL del gateway o del BFF.
8. Cambiar la contraseña de RabbitMQ y no subir `.env` ni `.pem` a GitHub.
