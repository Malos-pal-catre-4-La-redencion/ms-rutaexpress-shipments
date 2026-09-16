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
  -d '{"remitente":"Tienda RutaExpress","destinatario":"Juan Pérez","direccionDestino":"Av. Siempre Viva 123"}'

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

## Pruebas automatizadas

```bash
mvn test
```

Corren contra H2 en memoria (perfil `test`), no necesitan Neon ni conexión a
internet. Verifican: creación, que no se pueda saltar de `CREADO` a
`EN_RUTA` directamente (409), que las transiciones válidas sí funcionen, y
que un id inexistente dé 404.

## Siguiente paso

Este servicio queda "suelto" hasta que el BFF tenga un controller que lo
llame — eso es lo que sigue después de este microservicio.
