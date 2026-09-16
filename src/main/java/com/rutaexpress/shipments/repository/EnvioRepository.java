package com.rutaexpress.shipments.repository;

import com.rutaexpress.shipments.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
}
