package com.rutaexpress.shipments.web;

import com.rutaexpress.shipments.model.Envio;
import com.rutaexpress.shipments.service.EnvioService;
import com.rutaexpress.shipments.web.dto.CambiarEstadoRequest;
import com.rutaexpress.shipments.web.dto.CrearEnvioRequest;
import com.rutaexpress.shipments.web.dto.EnvioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Este servicio no valida JWT por su cuenta — confía en que solo el BFF lo
 * llama, dentro de la red interna (en EC2, ni siquiera va a tener un puerto
 * expuesto a internet). La autenticación y la autorización por rol ya
 * quedaron resueltas una vez, en el BFF; no hace falta repetirlas acá.
 */
@RestController
@RequestMapping("/envios")
public class EnvioController {

    private final EnvioService service;

    public EnvioController(EnvioService service) {
        this.service = service;
    }

    /** Cabecera con la que el BFF informa quién es el usuario autenticado (claim oid del JWT). */
    static final String CABECERA_USUARIO = "X-Usuario-Id";

    @PostMapping
    public ResponseEntity<EnvioResponse> crear(
            @Valid @RequestBody CrearEnvioRequest request,
            @RequestHeader(name = CABECERA_USUARIO, required = false) String usuarioId) {
        Envio creado = service.crear(request, normalizar(usuarioId));
        return ResponseEntity.status(HttpStatus.CREATED).body(EnvioResponse.desde(creado));
    }

    /** Sin parámetro devuelve todos los envíos; con ?creadoPor=<id> solo los de ese usuario. */
    @GetMapping
    public List<EnvioResponse> listar(@RequestParam(name = "creadoPor", required = false) String creadoPor) {
        String filtro = normalizar(creadoPor);
        List<Envio> envios = filtro == null ? service.listarTodos() : service.listarPorCreador(filtro);
        return envios.stream().map(EnvioResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public EnvioResponse obtener(@PathVariable Long id) {
        return EnvioResponse.desde(service.buscarPorId(id));
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    @PatchMapping("/{id}/estado")
    public EnvioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        Envio actualizado = service.cambiarEstado(id, request.nuevoEstado());
        return EnvioResponse.desde(actualizado);
    }
}
