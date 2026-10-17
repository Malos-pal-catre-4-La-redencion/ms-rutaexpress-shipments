package com.rutaexpress.shipments.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Registro de auditoría: un evento recibido por la cola q.auditoria. */
@Entity
@Table(name = "auditoria_eventos")
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Column(name = "payload", nullable = false, length = 4000)
    private String payload;

    @Column(name = "recibido_en", nullable = false)
    private Instant recibidoEn;

    protected AuditoriaEvento() {
        // JPA
    }

    public AuditoriaEvento(String routingKey, String payload) {
        this.routingKey = routingKey;
        this.payload = payload;
        this.recibidoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getRecibidoEn() {
        return recibidoEn;
    }
}
