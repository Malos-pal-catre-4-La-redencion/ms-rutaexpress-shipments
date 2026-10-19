package com.rutaexpress.shipments.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los mensajes que shipments publica viajan como JSON. El convertidor se aplica
 * solo al RabbitTemplate (productor): los listeners leen el cuerpo por su cuenta,
 * así no dependen de la cabecera __TypeId__ que escribe el otro microservicio.
 */
@Configuration
public class RabbitMessagingConfig {

    @Bean
    public RabbitTemplateCustomizer jsonRabbitTemplateCustomizer(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter convertidor = new Jackson2JsonMessageConverter(objectMapper);
        return template -> template.setMessageConverter(convertidor);
    }
}
