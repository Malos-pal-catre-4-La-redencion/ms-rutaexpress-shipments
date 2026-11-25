package com.rutaexpress.shipments.messaging.kafka;

import com.rutaexpress.shipments.messaging.kafka.dto.VehiculoEventoKafka;
import com.rutaexpress.shipments.model.EventoAnaliticaVehiculo;
import com.rutaexpress.shipments.repository.EventoAnaliticaVehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka de shipments. Reacciona a cada evento de vehículo
 * publicado por catalog guardando un registro de analítica — demuestra el
 * flujo productor (catalog) -> tópico -> consumidor (shipments) en el
 * sentido contrario al de EnvioEventoKafkaListener (catalog).
 */
@Component
public class VehiculoEventoKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(VehiculoEventoKafkaListener.class);

    private final EventoAnaliticaVehiculoRepository repository;

    public VehiculoEventoKafkaListener(EventoAnaliticaVehiculoRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(
            topics = "${rutaexpress.kafka.topics.eventos-vehiculos}",
            groupId = "${rutaexpress.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onVehiculoEvento(VehiculoEventoKafka evento) {
        if (evento.vehiculoId() == null || evento.patente() == null || evento.tipoEvento() == null) {
            // No recuperable: un reintento no lo va a arreglar. Se registra y se descarta.
            log.error("Evento de vehículo inválido, se descarta: {}", evento);
            return;
        }

        repository.save(new EventoAnaliticaVehiculo(
                evento.vehiculoId(), evento.patente(), evento.tipoEvento(),
                evento.disponible(), evento.ocurridoEn()));

        log.info("Analítica actualizada: vehículo {} ({}) -> {}",
                evento.patente(), evento.vehiculoId(), evento.tipoEvento());
    }
}
