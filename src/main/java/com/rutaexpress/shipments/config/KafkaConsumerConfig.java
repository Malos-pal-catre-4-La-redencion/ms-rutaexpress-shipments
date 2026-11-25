package com.rutaexpress.shipments.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * Consumidor Kafka de shipments: lee rutaexpress.eventos.vehiculos con el
 * grupo shipments-analitica-vehiculos (ver application.yml).
 *
 * Manejo de errores: igual que en catalog — ErrorHandlingDeserializer evita
 * que un JSON corrupto tumbe el contenedor, y DefaultErrorHandler reintenta
 * cada mensaje fallido 3 veces con 1s de espera antes de registrarlo en log
 * y avanzar el offset.
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> kafkaConsumerFactory(KafkaTopicsProperties props) {
        // Map.of() solo admite hasta 10 pares clave-valor; con 11 hay que usar ofEntries().
        Map<String, Object> config = Map.ofEntries(
                Map.entry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                Map.entry(ConsumerConfig.GROUP_ID_CONFIG, props.consumerGroup()),
                Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class),
                Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class),
                Map.entry(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class),
                Map.entry(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class),
                Map.entry(JsonDeserializer.TRUSTED_PACKAGES, "com.rutaexpress.shipments.messaging.kafka.dto"),
                Map.entry(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.rutaexpress.shipments.messaging.kafka.dto.VehiculoEventoKafka"),
                Map.entry(JsonDeserializer.USE_TYPE_INFO_HEADERS, false),
                Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                Map.entry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        );
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.RECORD);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Se agotaron los reintentos para el mensaje de la partición {} offset {}: {}",
                        record.partition(), record.offset(), exception.getMessage()),
                new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(org.apache.kafka.common.errors.SerializationException.class);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
