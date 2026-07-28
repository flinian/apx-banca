package com.practica.apx.connector.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementacion real del Kafka Connector (productor).
 *
 * @Profile("kafka"): este bean SOLO existe si la app arranca con el perfil
 * 'kafka' activo (requiere un broker disponible; ver docker-compose.yml).
 *
 * KafkaTemplate es a Kafka lo que JdbcTemplate a la BD: gestiona conexiones,
 * serializacion y envio; nosotros damos topico, clave y payload.
 */
@Component
@Profile("kafka")
public class KafkaTransferenciaEventPublisher implements TransferenciaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaTransferenciaEventPublisher.class);

    private final KafkaTemplate<String, EventoTransferencia> kafkaTemplate;
    private final String topico;

    public KafkaTransferenciaEventPublisher(
            KafkaTemplate<String, EventoTransferencia> kafkaTemplate,
            @Value("${app.kafka.topico-transferencias}") String topico) {
        this.kafkaTemplate = kafkaTemplate;
        this.topico = topico;
    }

    @Override
    public void publicar(EventoTransferencia evento) {
        // Clave = cuenta origen: garantiza que los eventos de una misma cuenta
        // caigan en la misma particion y conserven el ORDEN entre si.
        kafkaTemplate.send(topico, evento.numeroCuentaOrigen(), evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.error("Error publicando evento de transferencia en {}", topico, error);
                    } else {
                        log.info("Evento publicado en {} particion {} offset {}",
                                topico,
                                resultado.getRecordMetadata().partition(),
                                resultado.getRecordMetadata().offset());
                    }
                });
    }
}
