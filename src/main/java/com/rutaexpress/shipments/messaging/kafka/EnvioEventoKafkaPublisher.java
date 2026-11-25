package com.rutaexpress.shipments.messaging.kafka;

import com.rutaexpress.shipments.config.KafkaTopicsProperties;
import com.rutaexpress.shipments.messaging.kafka.dto.EnvioEventoKafka;
import com.rutaexpress.shipments.model.Envio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Productor Kafka de shipments. Usa el id del envío como key: todos los
 * eventos de un mismo envío caen siempre en la misma partición y catalog
 * los procesa en el orden en que ocurrieron.
 */
@Component
public class EnvioEventoKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(EnvioEventoKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties props;

    public EnvioEventoKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopicsProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void publicar(Envio envio) {
        String topico = props.topics().eventosEnvios();
        String key = String.valueOf(envio.getId());
        EnvioEventoKafka evento = new EnvioEventoKafka(
                envio.getId(), envio.getTrackingCode(), envio.getEstado().name(), Instant.now());

        kafkaTemplate.send(topico, key, evento).whenComplete((resultado, error) -> {
            if (error != null) {
                log.error("No se pudo publicar en Kafka el evento del envío {}: {}",
                        envio.getTrackingCode(), error.getMessage());
                return;
            }
            log.info("Evento Kafka publicado: {} -> envío {} (estado {}, partición {})",
                    topico, envio.getTrackingCode(), envio.getEstado(),
                    resultado.getRecordMetadata().partition());
        });
    }
}
