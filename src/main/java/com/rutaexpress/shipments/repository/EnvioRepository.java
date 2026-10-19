package com.rutaexpress.shipments.repository;

import com.rutaexpress.shipments.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvioRepository extends JpaRepository<Envio, Long> {

    List<Envio> findByCreadoPor(String creadoPor);
}
