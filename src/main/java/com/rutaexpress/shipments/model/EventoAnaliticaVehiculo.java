package com.rutaexpress.shipments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Historial de analítica construido a partir de rutaexpress.eventos.vehiculos
 * (Kafka). No es la fuente de verdad del vehículo (esa es catalog): es una
 * vista de solo lectura que shipments arma reaccionando a cada evento, para
 * poder llevar un histórico de disponibilidad de la flota.
 */
@Entity
@Table(name = "eventos_analitica_vehiculo")
public class EventoAnaliticaVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehiculo_id", nullable = false)
    private Long vehiculoId;

    @Column(name = "patente", nullable = false, length = 20)
    private String patente;

    @Column(name = "tipo_evento", nullable = false, length = 30)
    private String tipoEvento;

    @Column(name = "disponible", nullable = false)
    private boolean disponible;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;

    @Column(name = "recibido_en", nullable = false)
    private Instant recibidoEn;

    protected EventoAnaliticaVehiculo() {
        // JPA
    }

    public EventoAnaliticaVehiculo(Long vehiculoId, String patente, String tipoEvento,
                                   boolean disponible, Instant ocurridoEn) {
        this.vehiculoId = vehiculoId;
        this.patente = patente;
        this.tipoEvento = tipoEvento;
        this.disponible = disponible;
        this.ocurridoEn = ocurridoEn;
        this.recibidoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getVehiculoId() {
        return vehiculoId;
    }

    public String getPatente() {
        return patente;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public Instant getOcurridoEn() {
        return ocurridoEn;
    }

    public Instant getRecibidoEn() {
        return recibidoEn;
    }
}
