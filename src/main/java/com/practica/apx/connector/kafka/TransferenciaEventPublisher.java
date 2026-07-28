package com.practica.apx.connector.kafka;

/**
 * Contrato del Kafka Connector (lado productor).
 *
 * El Service depende de esta interfaz: no sabe si detras hay Kafka, otro
 * broker, o un simple log. Eso permite ejecutar la app sin infraestructura
 * (perfil por defecto) y con Kafka real (perfil 'kafka') sin tocar el negocio.
 */
public interface TransferenciaEventPublisher {

    /** Publica el evento de transferencia realizada. */
    void publicar(EventoTransferencia evento);
}
