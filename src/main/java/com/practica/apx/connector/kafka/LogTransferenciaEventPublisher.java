package com.practica.apx.connector.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementacion de respaldo del publisher: solo registra el evento en el log.
 *
 * @Profile("!kafka"): existe cuando el perfil 'kafka' NO esta activo. Permite
 * arrancar y demostrar la app sin ningun broker instalado, manteniendo intacto
 * el contrato del connector (el Service no nota la diferencia).
 */
@Component
@Profile("!kafka")
public class LogTransferenciaEventPublisher implements TransferenciaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LogTransferenciaEventPublisher.class);

    @Override
    public void publicar(EventoTransferencia evento) {
        log.info("[SIN BROKER] Evento de transferencia (se publicaria en Kafka): {}", evento);
    }
}
