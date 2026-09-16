package com.rutaexpress.shipments.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "envios")
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_code", nullable = false, unique = true, length = 20)
    private String trackingCode;

    @Column(name = "remitente", nullable = false)
    private String remitente;

    @Column(name = "destinatario", nullable = false)
    private String destinatario;

    @Column(name = "direccion_destino", nullable = false)
    private String direccionDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEnvio estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Envio() {
        // JPA
    }

    public Envio(String trackingCode, String remitente, String destinatario, String direccionDestino) {
        this.trackingCode = trackingCode;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.direccionDestino = direccionDestino;
        this.estado = EstadoEnvio.CREADO;
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    public void cambiarEstado(EstadoEnvio nuevoEstado) {
        this.estado = nuevoEstado;
        this.actualizadoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public String getRemitente() {
        return remitente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getDireccionDestino() {
        return direccionDestino;
    }

    public EstadoEnvio getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
