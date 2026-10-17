# ms-rutaexpress-shipments

Microservicio de envíos: CRUD + máquina de estados
(`CREADO → ACEPTADO → EN_BODEGA → EN_RUTA → ENTREGADO/CANCELADO`).

No valida JWT por su cuenta — confía en que solo `ms-rutaexpress-bff` lo
llama, en la red interna. La autenticación y la autorización por rol ya se
resuelven una vez, en el BFF.

## Configurar Neon

1. Copia `src/main/resources/application-local.yml.example` a
   `src/main/resources/application-local.yml` (ese sí está en `.gitignore`).
2. Reemplaza `url`, `username` y `password` con los datos reales de tu
   proyecto Neon.

## Correr en local

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Con `ddl-auto: update`, Hibernate crea la tabla `envios` sola la primera vez
que arranca — no hace falta escribir el DDL a mano.

## Probar con curl

```bash
# Crear un envío
curl -X POST http://localhost:8082/envios \
  -H "Content-Type: application/json" \
  -d '{"remitente":"Tienda RutaExpress","destinatario":"Juan Pérez","direccionDestino":"Av. Siempre Viva 123","pesoKg":12.5}'

# Listar
curl http://localhost:8082/envios

# Cambiar de estado (usa el id que te devolvió el POST)
curl -X PATCH http://localhost:8082/envios/1/estado \
  -H "Content-Type: application/json" \
  -d '{"nuevoEstado":"ACEPTADO"}'

# Probar una transición inválida (debería dar 409)
curl -i -X PATCH http://localhost:8082/envios/1/estado \
  -H "Content-Type: application/json" \
  -d '{"nuevoEstado":"ENTREGADO"}'
```

## Mensajería con RabbitMQ (EP2)

Shipments **produce** comandos y eventos y **consume** notificaciones y auditoría.
Variables de conexión: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`,
`RABBITMQ_PASSWORD` (las credenciales de `docker/.env` del repo
`ms-rutaexpress-rabbit-admin`).

### Cambio en el modelo: `pesoKg`

Crear un envío ahora exige `pesoKg` (> 0), porque catalog lo usa para elegir un
vehículo. Sin peso o con peso ≤ 0 la API responde 400 y **no se publica ningún mensaje**.
La columna `peso_kg` de la tabla `envios` es *nullable* para que Hibernate (`ddl-auto: update`)
pueda agregarla aunque ya existan envíos en Neon; los envíos antiguos quedan con peso vacío.

### Flujo

```
POST /envios (válido) ──▶ guarda el envío ──▶ responde 201 sin esperar
        │
        ├─ comando  rk envio.asignar-vehiculo ──▶ rutaexpress.commands (direct) ──▶ q.envio.comandos ──▶ catalog
        │
        └─ evento   rk envio.creado ───────────┐
PATCH /envios/{id}/estado ─ evento rk envio.estado.cambiado ─┤
catalog ─ evento rk envio.vehiculo.asignado ────────────────┤
                                               ▼
                                   rutaexpress.events (topic)
                                      │                    │
                           "envio.#"  ▼                    ▼  "#"
                             q.notificaciones         q.auditoria
                                      │                    │
                        NotificacionListener      AuditoriaListener ──▶ tabla auditoria_eventos (BD)
                                      │                    │
                       rechazado / vencido / desbordado    │
                                      ▼                    ▼
                    rutaexpress.dlx ──▶ q.notificaciones.dlq / q.auditoria.dlq ──▶ DeadLetterListener (log)
```

**Cuándo se usa cada exchange:** el *direct* para un comando con un único destinatario
(la routing key debe coincidir exacta). El *topic* para eventos que varios interesados
quieren recibir según un patrón: `envio.#` (notificaciones) recibe solo los de envíos;
`#` (auditoría) recibe absolutamente todos.

### Las tres colas

| Cola | Exchange | Binding | Consumidor | DLQ |
|---|---|---|---|---|
| `q.envio.comandos` | direct | `envio.asignar-vehiculo` | catalog | `q.envio.comandos.dlq` |
| `q.notificaciones` | topic | `envio.#` | `NotificacionListener` | `q.notificaciones.dlq` |
| `q.auditoria` | topic | `#` | `AuditoriaListener` | `q.auditoria.dlq` |

`q.envio.comandos` se declara también aquí (con argumentos idénticos a catalog) para que
ningún comando se pierda si shipments arranca antes que catalog.

### Dónde está cada cosa

| Pieza | Archivo |
|---|---|
| Nombres, routing keys y políticas | `application.yml` (`rutaexpress.mensajeria.*`) |
| Beans `Queue`, `Exchange` y `Binding` | `config/RabbitTopologyConfig` |
| Productor (comando y eventos) | `messaging/EnvioMensajesPublisher` |
| Consumidores | `messaging/NotificacionListener`, `messaging/AuditoriaListener` |
| Consumidor de las DLQ (log) | `messaging/DeadLetterListener` |
| Reintentos acotados | `messaging/ReintentoAcotado` |
| Lógica de negocio | `service/EnvioService`, `NotificacionService`, `AuditoriaService` |

### Cómo se maneja cada error

| Situación | Tipo | Resultado |
|---|---|---|
| Cuerpo que no es un objeto JSON | No recuperable | NACK sin requeue → DLQ |
| Evento sin `trackingCode` en notificaciones | No recuperable | NACK sin requeue → DLQ |
| La base de datos falla al auditar | Recuperable | Hasta 3 intentos; si persiste, NACK sin requeue → DLQ |
| Mensaje sin consumir (TTL) o cola llena | — | Expira o se desborda → DLQ |
| RabbitMQ caído al publicar | — | Se registra el error; el envío ya quedó guardado y la API responde 201 |

Los mensajes de las DLQ quedan en el log: `[DLQ] Mensaje no entregado. colaOrigen=... motivo=... contenido=...`.

### Probarlo en el Management UI

En **Exchanges → rutaexpress.events → Publish message**:

1. **Éxito** (routing key `envio.prueba`): `{"envioId":1,"trackingCode":"RE-TEST0001"}`
   → `[NOTIFICACIÓN] ...`, y una fila nueva en `auditoria_eventos`.
2. **No recuperable en notificaciones** (routing key `envio.prueba`): `{"x":1}`
   → `Notificación inválida (no recuperable)` y `[DLQ] ... q.notificaciones`. La auditoría sí lo guarda.
3. **Mensaje basura** (routing key `envio.prueba`): `esto no es json`
   → va a las DLQ de notificaciones y de auditoría.
4. **Solo auditoría** (routing key `sistema.alerta`): `{"x":1}`
   → no coincide con `envio.#`: llega únicamente a `q.auditoria`.

### Notas para la defensa

- **Prefetch = 10**: RabbitMQ no entrega más de 10 mensajes sin confirmar por consumidor, así que un consumidor lento no acumula miles de mensajes mientras otro está ocioso.
- **ACK manual**: el mensaje solo sale de la cola cuando el trabajo terminó (en auditoría, cuando ya se guardó en la base de datos).
- **El envío se guarda antes de publicar**: no se publica nada sobre un envío que no existe. Si RabbitMQ cae justo después, el envío queda sin comando; la solución completa es un patrón *outbox*.

## Pruebas automatizadas

```bash
mvn test
```

Corren contra H2 en memoria (perfil `test`), no necesitan Neon, RabbitMQ ni conexión a
internet (el publicador se reemplaza por un mock y los listeners no arrancan). Verifican: creación, que no se pueda saltar de `CREADO` a
`EN_RUTA` directamente (409), que las transiciones válidas sí funcionen, y
que un id inexistente dé 404.
