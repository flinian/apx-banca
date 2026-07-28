package com.practica.apx.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida tras ejecutar una transferencia: comprobante de la operacion.
 */
public record TransferenciaResponse(
        String numeroCuentaOrigen,
        String numeroCuentaDestino,
        BigDecimal monto,
        String moneda,
        BigDecimal saldoRestanteOrigen,
        String descripcion,
        LocalDateTime fecha
) {
}
