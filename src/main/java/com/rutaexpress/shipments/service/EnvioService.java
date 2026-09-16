package com.rutaexpress.shipments.service;

import com.rutaexpress.shipments.exception.EnvioNoEncontradoException;
import com.rutaexpress.shipments.exception.TransicionInvalidaException;
import com.rutaexpress.shipments.model.Envio;
import com.rutaexpress.shipments.model.EstadoEnvio;
import com.rutaexpress.shipments.repository.EnvioRepository;
import com.rutaexpress.shipments.web.dto.CrearEnvioRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EnvioService {

    private final EnvioRepository repository;

    public EnvioService(EnvioRepository repository) {
        this.repository = repository;
    }

    public Envio crear(CrearEnvioRequest request) {
        String trackingCode = generarTrackingCode();
        Envio envio = new Envio(trackingCode, request.remitente(), request.destinatario(), request.direccionDestino());
        return repository.save(envio);
    }

    public List<Envio> listarTodos() {
        return repository.findAll();
    }

    public Envio buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EnvioNoEncontradoException(id));
    }

    public Envio cambiarEstado(Long id, EstadoEnvio nuevoEstado) {
        Envio envio = buscarPorId(id);

        if (!envio.getEstado().puedeTransicionarA(nuevoEstado)) {
            throw new TransicionInvalidaException(envio.getEstado(), nuevoEstado);
        }

        envio.cambiarEstado(nuevoEstado);
        return repository.save(envio);
    }

    private String generarTrackingCode() {
        return "RE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
