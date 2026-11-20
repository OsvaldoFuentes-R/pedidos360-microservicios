# Guía de la presentación (EP6) — 5 a 10 minutos

Orden sugerido. Cada paso dice **qué mostrar**, **qué decir** y el punto de la pauta que cubre.

## 0. Preparación (antes de entrar a la sala)
- [ ] EC2 de RabbitMQ (nodo 1 y nodo 2) encendidas, cluster con 2 nodos (`rabbitmqctl cluster_status`).
- [ ] EC2 con Kafka (3 Zookeeper + 3 brokers) y Kafka UI arriba: `docker compose ps` todo `Up`.
- [ ] Backend (BFF + microservicios) y frontend desplegados y respondiendo.
- [ ] API Manager creado y apuntando al BFF; usuario de prueba creado en Azure.
- [ ] Una pestaña por cosa: Azure, API Gateway, EC2, RabbitMQ (15672), Kafka UI (8090), frontend.

## 1. Identidad y seguridad (≈2 min)
| Mostrar | Decir |
|---|---|
| Azure (IDaaS): el **tenant** CIAM y los **usuarios registrados** | "Usamos Microsoft Entra External ID como IDaaS; aquí está el tenant y los usuarios." |
| Frontend: botón de login → pantalla de Microsoft → vuelve con sesión | "Flujo OAuth 2.0 / OpenID Connect con MSAL; el frontend obtiene un JWT." |
| DevTools → Network: la llamada lleva `Authorization: Bearer ...` y el JWT en jwt.ms | "El frontend envía el JWT en cada llamada." |
| API Manager (API Gateway): creación de la instancia, rutas hacia el BFF y el **JWT authorizer** (issuer + audience) | "Todas las llamadas del frontend pasan por el API Manager." |
| Postman/curl **sin token** → 401; **con token** → 200 | "El gateway rechaza lo inválido y acepta lo correcto. Además el BFF vuelve a validar firma, vigencia y audiencia (defensa en profundidad)." |

Datos para configurar el authorizer: *issuer* = el campo `issuer` de
`https://<TENANT>.ciamlogin.com/<TENANT_ID>/v2.0/.well-known/openid-configuration` (cópialo de ahí, no lo escribas a mano);
*audience* = client id de la app "API" registrada en Azure. En el BFF: `JWT_ISSUER` y `JWT_AUDIENCE`.

## 2. Despliegue en la nube (≈1 min)
Consola AWS: instancias EC2 en *running* (BFF + microservicios, RabbitMQ ×2, Kafka) y el frontend publicado.
"Backend y frontend están desplegados, activos e integrados": abrir el frontend desplegado y listar pedidos.

## 3. RabbitMQ (≈2 min)
| Mostrar | Decir |
|---|---|
| Panel → *Overview*: **2 nodos** en el cluster (`rabbit@rabbitmq-nodo1`, `-nodo2`) | "Cluster de 2 nodos con política de espejado `ha-pedidos`." |
| *Queues*: `pedidos.registro`, `pedidos.notificaciones`, `pedidos.tickets` y sus 3 DLQ `pedidos.*.dlq` | "3 colas, cada una con su DLQ." |
| *Exchanges*: `pedidos360.exchange` (**direct**) y `pedidos360.dlx` (**topic**) | "El direct enruta por routing key exacta; el topic enruta los mensajes rechazados a su DLQ." |
| Publicar `esto no es json` en `pedidos360.exchange` / `pedido.registro` → aparece en `pedidos.registro.dlq`; log: "Mensaje FALLIDO … NACK" | "El consumidor hace NACK sin reencolar y RabbitMQ lo manda a la DLQ; queda registrado en el log." |
| Publicar un pedido válido `{"clienteId":1,"items":[{"productoId":1,"cantidad":2}]}` → se procesa | Flujo completo con ACK. |
| (Opcional) `docker stop rabbitmq-nodo1` y repetir | Tolerancia a la caída de un nodo. |
| ms-rabbitmq-admin: `POST /rabbit/colas`, `POST /rabbit/exchanges`, `POST /rabbit/bindings`, y sus `DELETE` | "Microservicio administrador: crea y elimina colas, exchanges y bindings." |

## 4. Kafka (≈3 min — es la parte que más puntaje tiene)
**a) Arquitectura (10 %)** — `docker compose ps` y diagrama de `docs/kafka.md`:
3 Zookeeper = quórum (tolera 1 caído, elige controlador); 3 brokers = cada partición tiene 3 réplicas;
`min.insync.replicas=2` + `acks=all` = un mensaje confirmado está en ≥2 brokers.
**b) Docker Compose (requisito)** — mostrar `infra/kafka/docker-compose.yml` levantando los servicios.
**c) Kafka UI (5 %)** — *Brokers* (3), *Topics*: **`pedidos.eventos` e `inventario.movimientos` con 3 particiones y 3 réplicas**,
*Messages* (ver clave, partición, offset), *Consumers* (grupos y lag).
**d) Tópicos (10 %)** — por qué 3 particiones (paralelismo: 3 consumidores por grupo) y 3 réplicas (resiliencia);
clave = id → orden por pedido; retención 7 d / 1 d.
**e) Flujo y eventos en tiempo real (12 % + 12 %)** — crear un pedido: en los logs de ms-pedidos
`[KAFKA] Evento … publicado en pedidos.eventos (partición X, offset Y)`; en ms-analitica
`[KAFKA] grupo=analitica-kpis … [KPI] Pedido 7 contabilizado`. Mostrar `GET :8088/analitica/kpis` cambiando.
Avanzar el ticket (`PUT :8085/tickets/1/estado {"estado":"EN_RUTA"}`) → nuevo evento `TICKET_ESTADO_CAMBIADO` en Kafka UI.
**f) Diseño de eventos (6 %)** — mostrar un mensaje en Kafka UI: `eventId`, `tipo`, clave de negocio, datos necesarios
para el consumidor sin llamar a otro servicio.
**g) Administración y monitoreo (15 % + 15 %)** — Kafka UI → *Consumers* → `analitica-kpis`: **offsets, lag y distribución**
por partición. Explicar: lag 0 = al día; lag creciente = el consumidor no da abasto o está caído → escalar instancias o
revisar errores. Probar: detener ms-analitica, crear 3 pedidos, ver el lag subir, reiniciar y ver cómo baja a 0.
`GET :8086/kafka/grupos/analitica-kpis` muestra lo mismo por API.
**h) Casos de uso (15 %)** — ver la tabla de abajo.
**i) Tolerancia a fallos** — `docker stop kafka-2`: en Kafka UI cambian los líderes y todo sigue funcionando.
**j) Error handling** — publicar `{"basura":true}` en `pedidos.eventos` desde Kafka UI → log del consumidor, mensaje en
`pedidos.eventos.dlt` y el flujo sigue.

### Casos de uso que justifican cada decisión de Kafka
| Caso de uso | Tópico / grupo | Configuración y por qué |
|---|---|---|
| Reportería de ventas (KPIs) | `pedidos.eventos` / `analitica-kpis` | retención 7 d: se puede **reprocesar** el historial reiniciando el grupo (`auto-offset-reset=earliest`) |
| Auditoría de lo ocurrido | ambos / `analitica-auditoria` | grupo propio: lee **todo** sin afectar a los otros (pub/sub) |
| Alerta de stock bajo | `inventario.movimientos` / `analitica-inventario` | retención 1 d: dato operativo; clave = productoId → orden por producto |
| Escalar la analítica | `--scale ms-analitica=3` | 3 particiones → 3 instancias del grupo reparten el trabajo |
| Servicio que se cae | cualquiera | los mensajes quedan en el tópico; al volver, el grupo continúa desde su último offset |

## 5. Preguntas típicas del profesor
- **¿Por qué Kafka si ya hay RabbitMQ?** RabbitMQ entrega comandos que se consumen y desaparecen; Kafka guarda un log
  de hechos que varios servicios releen (analítica, auditoría) y se puede reprocesar. Se complementan.
- **¿Qué pasa si se cae un broker?** Cada partición tiene 3 réplicas; con `min.insync.replicas=2` seguimos escribiendo con 2 brokers.
- **¿Y si se caen 2?** Las particiones con ISR < 2 dejan de aceptar escrituras (con `acks=all`): se prioriza no perder datos.
- **¿Para qué sirven los Zookeeper?** Metadatos, elección del controlador y configuración. Con 3 hay quórum (2 de 3).
- **¿Qué es el lag?** Mensajes publicados que el grupo aún no procesa (último offset − offset confirmado).
- **¿Qué garantiza `eventId`?** Idempotencia: Kafka entrega "al menos una vez"; el consumidor ignora repetidos.
- **Límite honesto:** los 3 Zookeeper y 3 brokers de la demo corren en una sola máquina; la tolerancia a fallos de
  *host* requiere repartirlos en varias EC2. No hay patrón *outbox*: si Kafka cae justo tras guardar un pedido, ese
  evento se pierde (se registra en el log).
