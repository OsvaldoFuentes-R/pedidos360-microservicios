# Despliegue de Pedidos360 en AWS (paso a paso)

Guía para montar todo desde cero, en este orden. **No avances de fase hasta que la verificación de la anterior salga bien.**

## Arquitectura

```
Frontend (localhost:4200) ──HTTPS──► API Gateway (JWT) ──HTTP :8080──► EC2 app (BFF + 8 microservicios)
                                                                           │        │          │
                                                                  AMQP 5672│  Kafka │ 9092-94  │ 5432
                                                                           ▼        ▼          ▼
                                                        EC2 rabbitmq-nodo1/2   EC2 kafka    RDS PostgreSQL
                                                        (cluster, 2 nodos)  (3 ZK + 3 brokers)  (6 esquemas)
```

| Fase | Recurso | Tipo sugerido | Disco |
|---|---|---|---|
| 1 | 2 EC2 `rabbitmq-nodo1`, `rabbitmq-nodo2` | t3.small | 20 GB |
| 2 | 1 EC2 `kafka` | t3.large (8 GB RAM) | 30 GB |
| 3 | 1 RDS PostgreSQL `pedidos360-db` | db.t3.micro | 20 GB |
| 4 | 1 EC2 `app` | t3.medium (4 GB) o t3.large | 30 GB |
| 5 | API Gateway (HTTP API) | — | — |

Sistema operativo de todas las EC2: **Ubuntu Server 24.04 LTS**, usuario de acceso `ubuntu`.

### Cuidados generales
- **Costo:** 4 EC2 + 1 RDS cuestan dinero mientras estén encendidas. Cuando no las uses, **deténlas** (Stop), no las termines (Terminate).
- **AWS Academy (Learner Lab):** el laboratorio se apaga solo y tiene presupuesto limitado. Las IP públicas que no sean Elastic IP cambian al reiniciar. Si algún servicio (por ejemplo roles IAM) está bloqueado, avisa: se resuelve con otra vía.
- **Siempre usa IP privadas** (las `172.31.x.x`) entre las máquinas. Solo el BFF necesita IP pública.
- **Nunca subas a GitHub** el `.env` ni el `.pem`.
- Todo en **la misma región** y en **la misma VPC** (la VPC por defecto sirve).

### Hoja de datos (ve completándola)
| Dato | Valor |
|---|---|
| Región | |
| IP privada rabbitmq-nodo1 | |
| IP privada rabbitmq-nodo2 | |
| IP privada kafka | |
| IP pública (privada) app | |
| Elastic IP del BFF | |
| Endpoint de RDS | |
| URL del API Gateway | |

---

## Fase 0: Preparación

### 0.1 Región
Arriba a la derecha de la consola de AWS elige una región (por ejemplo **N. Virginia, us-east-1**) y no la cambies.

### 0.2 Par de claves (acceso SSH)
1. **EC2 → Network & Security → Key pairs → Create key pair**.
2. Nombre `pedidos360`, tipo **RSA**, formato **.pem**. Se descarga el archivo: guárdalo (por ejemplo en `C:\Users\Osvaldo Fuentes\.ssh\pedidos360.pem`).
3. En PowerShell, deja el archivo con permisos solo para ti (si no, SSH dice *UNPROTECTED PRIVATE KEY FILE*):
```powershell
$k = "$env:USERPROFILE\.ssh\pedidos360.pem"
icacls $k /inheritance:r
icacls $k /grant:r "$($env:USERNAME):(R)"
```

### 0.3 Security Groups
**EC2 → Security Groups → Create security group**, en la VPC por defecto. Crea cuatro, con las reglas de **entrada** de la tabla. "Mi IP" es la opción **My IP** del menú. Crea primero los cuatro vacíos y después edita las reglas, porque algunas se referencian entre sí.

| Grupo | Puerto | Origen | Para qué |
|---|---|---|---|
| `sg-rabbitmq` | 22 | Mi IP | SSH |
| | 5672 | `sg-app` | AMQP desde los servicios |
| | 15672 | Mi IP **y** `sg-app` | Panel web (y API de gestión para `ms-rabbitmq-admin`) |
| | 4369 y 25672 | `sg-rabbitmq` (el mismo grupo) | Comunicación entre nodos |
| `sg-kafka` | 22 | Mi IP | SSH |
| | 9092, 9093, 9094 | `sg-app` | Brokers desde los servicios |
| | 8090 | Mi IP | Kafka UI |
| `sg-app` | 22 | Mi IP | SSH |
| | 8080 | `0.0.0.0/0` | BFF (lo llama el API Gateway) |
| `sg-rds` | 5432 | `sg-app` | PostgreSQL |

Reglas de salida: déjalas por defecto (todo permitido).

> Los puertos 8081 a 8088 de `sg-app` **no** se abren a Internet. Si quieres usar Postman contra los microservicios, usa un túnel SSH (ver fase 4.8) o abre esos puertos solo a "Mi IP".

> Tu IP cambia si te conectas desde otra red; si pierdes el acceso por SSH, actualiza la regla "Mi IP".

**Verificación fase 0:** existen el key pair `pedidos360` y los 4 Security Groups.

---

## Fase 1: Cluster RabbitMQ (2 nodos)

Usa **RabbitMQ 3.13 con colas espejadas** (no quorum queues: con 2 nodos no hay mayoría).

### 1.1 Crear las dos EC2
**EC2 → Launch instance**, dos veces:
- Nombre `rabbitmq-nodo1` y `rabbitmq-nodo2`
- AMI: **Ubuntu Server 24.04 LTS**
- Tipo: **t3.small**
- Key pair: `pedidos360`
- Network settings → **Select existing security group** → `sg-rabbitmq`
- Storage: 20 GB

Cuando estén *Running*, anota sus **IP privadas** (columna *Private IPv4 address*) en la hoja de datos.

### 1.2 Instalar Docker (en cada nodo)
Conéctate (cambia `IP_PUBLICA` por la IP pública del nodo):
```powershell
ssh -i "$env:USERPROFILE\.ssh\pedidos360.pem" ubuntu@IP_PUBLICA
```
Dentro de la EC2:
```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu
exit
```
Vuelve a conectarte por SSH (para que se aplique el grupo `docker`) y comprueba con `docker ps`.

### 1.3 Traer el proyecto (en cada nodo)
```bash
git clone https://github.com/OsvaldoFuentes-R/pedidos360-microservicios.git
cd pedidos360-microservicios/pedidos360-backend/infra/rabbitmq
```
(Si el repositorio es privado, GitHub pedirá usuario y un *Personal Access Token* como contraseña.)

### 1.4 Archivo `.env` (en cada nodo, **idéntico** en los dos)
```bash
cp .env.example .env
nano .env
```
Completa:
```
IP_NODO1=<IP privada de rabbitmq-nodo1>
IP_NODO2=<IP privada de rabbitmq-nodo2>
ERLANG_COOKIE=<una cadena larga y aleatoria; la misma en los dos nodos>
RABBITMQ_USER=pedidos
RABBITMQ_PASSWORD=<una clave fuerte; anótala>
```
Guarda con `Ctrl+O`, Enter, `Ctrl+X`.

### 1.5 Levantar los nodos
- En **nodo 1**: `docker compose -f docker-compose.nodo1.yml up -d`
- En **nodo 2**: `docker compose -f docker-compose.nodo2.yml up -d`

Espera 30 segundos y comprueba con `docker ps` que cada uno esté *Up*.

### 1.6 Unir el nodo 2 al nodo 1 (se ejecuta **solo en la EC2 del nodo 2**)
```bash
docker exec rabbitmq-nodo2 rabbitmqctl stop_app
docker exec rabbitmq-nodo2 rabbitmqctl reset
docker exec rabbitmq-nodo2 rabbitmqctl join_cluster rabbit@rabbitmq-nodo1
docker exec rabbitmq-nodo2 rabbitmqctl start_app
```

### 1.7 Política de espejado (en cualquier nodo)
```bash
docker exec rabbitmq-nodo1 rabbitmqctl set_policy ha-pedidos "^pedidos\." \
  '{"ha-mode":"all","ha-sync-mode":"automatic"}' --apply-to queues
```

### Verificación fase 1
```bash
docker exec rabbitmq-nodo1 rabbitmqctl cluster_status
```
En *Running Nodes* deben aparecer **los dos nodos**. En el navegador, `http://IP_PUBLICA_NODO1:15672` (usuario `pedidos` y tu clave) → pestaña **Overview** → *Nodes* debe mostrar 2 nodos en verde.

> Las colas (`pedidos.registro`, etc.) las crean los microservicios al arrancar en la fase 4; ahora no habrá ninguna.

---

## Fase 2: Kafka (3 Zookeeper + 3 brokers)

### 2.1 Crear la EC2
- Nombre `kafka`, Ubuntu 24.04, tipo **t3.large (8 GB)**, key pair `pedidos360`, Security Group `sg-kafka`, disco 30 GB.
- Anota su **IP privada**.

### 2.2 Instalar Docker y traer el proyecto
Igual que en 1.2 y 1.3, pero entra a:
```bash
cd pedidos360-microservicios/pedidos360-backend/infra/kafka
```

### 2.3 Archivo `.env`
```bash
cp .env.example .env
nano .env
```
```
KAFKA_PUBLIC_HOST=<IP PRIVADA de esta EC2 de Kafka>
KAFKA_UI_USER=admin
KAFKA_UI_PASSWORD=<una clave fuerte>
```
> `KAFKA_PUBLIC_HOST` es la dirección que los brokers anuncian a los clientes. Debe ser la IP privada de esta EC2 (los microservicios están en la misma VPC). Si pones `localhost`, los servicios no se podrán conectar.

### 2.4 Levantar Kafka y Kafka UI
```bash
docker compose up -d
docker compose -f docker-compose.kafka-ui.yml up -d
```
El contenedor `kafka-init` crea los tópicos y termina (es normal que quede en estado *Exited*).

### Verificación fase 2
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
docker exec pedidos360-kafka-kafka-1-1 kafka-topics --bootstrap-server kafka-1:29092 --describe --topic pedidos.eventos
```
Debes ver 3 Zookeeper y 3 brokers *Up*, y el tópico con `PartitionCount: 3` y `ReplicationFactor: 3`. En el navegador, `http://IP_PUBLICA_KAFKA:8090` (login con tu usuario y clave) → **Topics** muestra `pedidos.eventos` e `inventario.movimientos`.

---

## Fase 3: Base de datos en la nube (RDS PostgreSQL)

### 3.1 Crear la base
**RDS → Create database**:
- **Standard create** → motor **PostgreSQL** (versión 16.x)
- Template: **Free tier** o **Dev/Test**
- DB instance identifier: `pedidos360-db`
- Master username: `pedidosadmin`; Master password: una clave fuerte (anótala)
- Instance class: **db.t3.micro**
- Storage: 20 GB
- Connectivity: VPC por defecto, **Public access: No**, VPC security group: **`sg-rds`** (quita el *default*)
- Additional configuration → **Initial database name: `pedidos360`**
- Desactiva backups automáticos si es solo para la evaluación (ahorra costo)

Tarda varios minutos. Cuando esté *Available*, copia el **Endpoint** (algo como `pedidos360-db.xxxx.us-east-1.rds.amazonaws.com`) a la hoja de datos.

### 3.2 Crear los esquemas
La base se alcanza solo desde la EC2 `app` (fase 4). Haz 4.1 a 4.3 y luego, en esa EC2:
```bash
sudo apt-get install -y postgresql-client
cd ~/pedidos360-microservicios/pedidos360-backend
psql "host=ENDPOINT dbname=pedidos360 user=pedidosadmin sslmode=require" -f infra/aws/esquemas.sql
```
Pide la contraseña y debe responder `CREATE SCHEMA` seis veces. Comprueba con:
```bash
psql "host=ENDPOINT dbname=pedidos360 user=pedidosadmin sslmode=require" -c "\dn"
```
Deben aparecer `productos, clientes, pedidos, notificaciones, despacho, analitica`.

> Hibernate crea las **tablas** solo (`ddl-auto=update`), pero no los esquemas; por eso este paso.

---

## Fase 4: EC2 `app` (BFF + microservicios)

### 4.1 Crear la EC2
- Nombre `app`, Ubuntu 24.04, **t3.medium** (o t3.large si va justa), key pair `pedidos360`, Security Group `sg-app`, disco 30 GB.

### 4.2 IP fija (Elastic IP)
Sin esto, la IP pública cambia al reiniciar y el API Gateway deja de funcionar.
**EC2 → Elastic IPs → Allocate Elastic IP address → Allocate**, luego **Actions → Associate Elastic IP address** → instancia `app`. Anota la IP en la hoja de datos.

### 4.3 Docker, swap y proyecto
```bash
ssh -i "$env:USERPROFILE\.ssh\pedidos360.pem" ubuntu@ELASTIC_IP
```
```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu
# 2 GB de swap: compilar 9 proyectos Maven necesita memoria
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
exit
```
Reconecta y trae el proyecto:
```bash
git clone https://github.com/OsvaldoFuentes-R/pedidos360-microservicios.git
cd pedidos360-microservicios/pedidos360-backend
```

### 4.4 Archivo `.env`
```bash
cp .env.aws.example .env
nano .env
```
Completa con los datos de tu hoja:
```
RABBITMQ_NODO1=<IP privada nodo1>
RABBITMQ_NODO2=<IP privada nodo2>
KAFKA_HOST=<IP privada de la EC2 kafka>
RABBITMQ_USER=pedidos
RABBITMQ_PASSWORD=<la clave de la fase 1>
DB_HOST=<endpoint de RDS>
DB_NAME=pedidos360
DB_USER=pedidosadmin
DB_PASSWORD=<la clave de la fase 3>
CORS_ORIGENES=http://localhost:4200
```
Haz primero la **3.2** (crear los esquemas) si todavía no la hiciste.

### 4.5 Levantar los servicios
```bash
COMPOSE_PARALLEL_LIMIT=2 docker compose -f docker-compose.aws.yml up --build -d
```
La primera vez tarda entre 10 y 20 minutos (compila los 9 proyectos). `COMPOSE_PARALLEL_LIMIT=2` evita que se agote la memoria.

### 4.6 Verificar que todo arrancó
```bash
docker compose -f docker-compose.aws.yml ps
for p in 8080 8081 8082 8083 8084 8085 8086 8087 8088; do
  echo -n "$p -> "; curl -s localhost:$p/actuator/health; echo
done
```
Los 9 deben responder `{"status":"UP"}`. Si alguno falla: `docker compose -f docker-compose.aws.yml logs --tail 60 NOMBRE`.

### 4.7 Prueba de punta a punta (sin frontend)
Publica un pedido desde esta EC2 (reemplaza `NODO1`, `CLAVE`):
```bash
curl -u pedidos:CLAVE -H "content-type: application/json" -X POST \
  http://NODO1:15672/api/exchanges/%2F/pedidos360.exchange/publish \
  -d '{"properties":{},"routing_key":"pedido.registro","payload":"{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":1}]}","payload_encoding":"string"}'
```
Debe responder `{"routed":true}`. Luego:
```bash
curl -s localhost:8083/pedidos     # el pedido registrado en RDS
curl -s localhost:8085/tickets     # el ticket generado
curl -s localhost:8088/analitica/kpis   # KPIs desde Kafka
```
En el panel de RabbitMQ (nodo 1) ya existen las colas `pedidos.*` y sus `.dlq`, y en Kafka UI hay mensajes en `pedidos.eventos`.

### 4.8 Acceder a los puertos internos desde tu PC (túnel SSH)
Para Postman o los servicios de administración, sin abrir puertos:
```powershell
ssh -i "$env:USERPROFILE\.ssh\pedidos360.pem" -L 8086:localhost:8086 -L 8087:localhost:8087 -L 8083:localhost:8083 ubuntu@ELASTIC_IP
```
Mientras esa ventana esté abierta, `http://localhost:8086/kafka/cluster` o `http://localhost:8087/rabbit/colas` en tu PC llegan a la EC2.

### Verificación fase 4
Desde **tu PC** (no la EC2): `http://ELASTIC_IP:8080/actuator/health` debe devolver `{"status":"UP"}`, y `http://ELASTIC_IP:8080/api/pedidos` debe responder **401** (exige token: es lo correcto).

---

## Fase 5: API Gateway con JWT

### 5.1 Crear la API
**API Gateway → Create API → HTTP API → Build**:
- Integrations → **Add integration** → tipo **HTTP**, método **ANY**, URL `http://ELASTIC_IP:8080/api/{proxy}`
- API name: `pedidos360-api`
- Routes: método **ANY**, recurso `/api/{proxy+}`
- Stage: `$default` con **Auto-deploy** activado
- **Create**. Anota la **Invoke URL**.

### 5.2 CORS
API → **CORS → Configure**:
- Access-Control-Allow-Origin: `http://localhost:4200`
- Access-Control-Allow-Methods: `GET, POST, PUT, DELETE, OPTIONS`
- Access-Control-Allow-Headers: `authorization, content-type`
- **Save**

### 5.3 Autorizador JWT
1. **Authorization → Manage authorizers → Create** → tipo **JWT**.
2. Name `azure-ciam`; Identity source `$request.header.Authorization`.
3. **Issuer URL:** `https://39eb9615-c754-4ef1-96f8-4f61494df782.ciamlogin.com/39eb9615-c754-4ef1-96f8-4f61494df782/v2.0`
4. **Audience:** `d1a052a1-66c7-4847-b5e2-46fdb86c94f6` (el client id de la app).
5. **Create**, y luego en **Attach authorizers to routes**, asócialo a la ruta `ANY /api/{proxy+}`.

> **Comprueba el issuer real:** inicia sesión en el frontend, copia el token (F12 → Network → petición `pedidos` → cabecera `authorization`, sin la palabra `Bearer`) y pégalo en <https://jwt.ms>. El campo `iss` debe ser **exactamente** el Issuer URL de arriba; el campo `aud`, el client id. Si difieren, usa los valores del token. Un issuer incorrecto produce 401 en todas las llamadas.

### Verificación fase 5
```powershell
Invoke-WebRequest "https://ID.execute-api.REGION.amazonaws.com/api/pedidos"            # sin token: 401
```
Con el token (cabecera `Authorization: Bearer ...`) debe devolver **200** con la lista de pedidos. Con Postman: GET a esa URL, pestaña Authorization → Bearer Token.

Si el navegador reporta error de CORS en la petición previa (OPTIONS) con código 401, agrega una ruta aparte `OPTIONS /api/{proxy+}` **sin** autorizador.

---

## Fase 6: Frontend contra AWS

En `Frontend/src/environments/environment.ts`:
```ts
apiUrl: 'https://ID.execute-api.REGION.amazonaws.com/api',
```
Ejecuta `npx ng serve`, abre `http://localhost:4200`, inicia sesión y prueba:
1. **Nuevo pedido** → crear uno (mensaje `EN_COLA`).
2. **Pedidos** → aparece a los pocos segundos, con su ticket.
3. **Reportes** y **Auditoría** → muestran datos (vienen de Kafka).

Para volver a trabajar en local, cambia `apiUrl` de nuevo a `http://localhost:8080/api`.

---

## Pruebas para mostrar en la evaluación (en AWS)

| Prueba | Cómo |
|---|---|
| Cluster RabbitMQ | Panel del nodo 1 → Overview → 2 nodos |
| Caída de un nodo RabbitMQ | En la EC2 del nodo 1: `docker stop rabbitmq-nodo1`; crea un pedido desde el frontend: sigue funcionando por el nodo 2. Luego `docker start rabbitmq-nodo1` |
| Caída de un broker Kafka | En la EC2 de Kafka: `docker stop pedidos360-kafka-kafka-2-1`; crea un pedido: sigue funcionando. Luego `docker start ...` |
| Particiones y réplicas | Kafka UI → Topics → `pedidos.eventos` (3 / 3) |
| Grupos de consumidores | Kafka UI → Consumers → `analitica-kpis`, `analitica-inventario`, `analitica-auditoria` |
| DLQ | Panel RabbitMQ → exchange `pedidos360.exchange` → publicar `texto sin json` con routing key `pedido.registro` → crece `pedidos.registro.dlq` |
| API Manager + JWT | Llamada sin token → 401; con token → 200 |
| Base en la nube | `psql ... -c "select * from pedidos.pedidos"` (o el panel de RDS) |
| Servicios admin | Túnel SSH (4.8) → `/kafka/cluster`, `/rabbit/colas` |

## Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `UNPROTECTED PRIVATE KEY FILE` | permisos del `.pem` | comandos `icacls` de 0.2 |
| Un servicio no se conecta a Kafka | `KAFKA_PUBLIC_HOST` mal puesto | debe ser la IP privada de la EC2 de Kafka; reinicia Kafka con `docker compose up -d --force-recreate` |
| `Connection refused` a RabbitMQ | Security Group | `sg-rabbitmq` debe permitir 5672 desde `sg-app` |
| Nodo 2 no se une | cookie distinta o puertos 4369/25672 cerrados | misma `ERLANG_COOKIE`; regla autorreferenciada |
| `schema "pedidos" does not exist` | no se crearon los esquemas | paso 3.2 |
| `too many connections` en RDS | muchas conexiones | ya limitado a 4 por servicio; reinicia los servicios |
| `OutOfMemory` al compilar | poca RAM | confirma el swap (`free -h`) o usa t3.large |
| 401 en todo desde el API Gateway | issuer o audience incorrectos | compara con el token en jwt.ms |
| Error CORS en el navegador | CORS no configurado en el gateway | paso 5.2 |
| El gateway da 503/504 | el BFF no responde | `curl http://ELASTIC_IP:8080/actuator/health` y revisa `sg-app` |
| Dejó de funcionar tras apagar y encender | IP pública cambió | usa la Elastic IP; las privadas no cambian |

## Apagar y limpiar
- Para pausar: **Stop** en las 4 EC2 y en la RDS (RDS se enciende sola a los 7 días).
- Para eliminar todo al terminar: API Gateway, RDS (sin snapshot final si no la necesitas), las 4 EC2, la Elastic IP (**release**) y los Security Groups.
- Cambia las claves de ejemplo y no reutilices las contraseñas de este proyecto.
