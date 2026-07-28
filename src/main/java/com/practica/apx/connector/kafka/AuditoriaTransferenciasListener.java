package com.practica.apx.connector.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de auditoria (perfil 'kafka').
 *
 * Simula un sistema DOWNSTREAM independiente (auditoria/cumplimiento) que
 * reacciona a los eventos sin que el productor sepa de su existencia: ese es
 * el desacople que aporta el patron pub/sub.
 *
 * @KafkaListener: Spring suscribe este metodo al topico y deserializa cada
 * mensaje JSON a EventoTransferencia automaticamente.
 */
@Component
@Profile("kafka")
public class AuditoriaTransferenciasListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaTransferenciasListener.class);

    @KafkaListener(
            topics = "${app.kafka.topico-transferencias}",
            groupId = "auditoria-transferencias")
    public void onTransferencia(EventoTransferencia evento) {
        log.info("[AUDITORIA] Transferencia registrada: {} -> {} por {} {} ({})",
                evento.numeroCuentaOrigen(),
                evento.numeroCuentaDestino(),
                evento.monto(),
                evento.moneda(),
                evento.descripcion());
    }
}
