package com.practica.apx.connector.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de dominio: "se realizo una transferencia".
 *
 * Se publica en Kafka para que OTROS sistemas (auditoria, notificaciones,
 * fraude...) reaccionen sin acoplarse a este microservicio. El evento lleva
 * los datos que un consumidor necesita, no referencias internas.
 */
public record EventoTransferencia(
        String numeroCuentaOrigen,
        String numeroCuentaDestino,
        BigDecimal monto,
        String moneda,
        String descripcion,
        LocalDateTime fecha
) {
}
