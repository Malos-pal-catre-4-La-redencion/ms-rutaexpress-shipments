package com.rutaexpress.shipments.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
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

    // Nullable a propósito: la tabla ya puede tener envíos creados antes de agregar el peso.
    // La obligatoriedad se exige en CrearEnvioRequest, no en la base de datos.
    @Column(name = "peso_kg", precision = 10, scale = 2)
    private BigDecimal pesoKg;

    // Identificador (claim "oid" de Entra ID) del usuario que creó el envío. Lo informa el BFF.
    // Nullable: los envíos anteriores a este campo no tienen dueño y solo los ven Admin y Despachador.
    @Column(name = "creado_por", length = 100)
    private String creadoPor;

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

    public Envio(String trackingCode, String remitente, String destinatario, String direccionDestino, BigDecimal pesoKg) {
        this(trackingCode, remitente, destinatario, direccionDestino, pesoKg, null);
    }

    public Envio(String trackingCode, String remitente, String destinatario, String direccionDestino,
                 BigDecimal pesoKg, String creadoPor) {
        this.creadoPor = creadoPor;
        this.trackingCode = trackingCode;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.direccionDestino = direccionDestino;
        this.pesoKg = pesoKg;
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

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public String getCreadoPor() {
        return creadoPor;
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
