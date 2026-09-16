package com.rutaexpress.shipments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ShipmentsApplication {

    private static final Logger log = LoggerFactory.getLogger(ShipmentsApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ShipmentsApplication.class, args);
    }

    @Bean
    CommandLineRunner startupBanner(@Value("${server.port}") String port) {
        return args -> {
            log.info("🚚 ms-rutaexpress-shipments arriba en el puerto {}", port);
            log.info("📦 Endpoints: POST /envios · GET /envios · GET /envios/{{id}} · PATCH /envios/{{id}}/estado");
            log.info("🔒 Sin validación de JWT propia — confía en que solo el BFF lo llama");
        };
    }
}
