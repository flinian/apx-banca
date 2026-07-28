package com.practica.apx.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Objeto de dominio: un movimiento contable de una cuenta.
 * El id es null hasta que el "mainframe" (BD) lo genera al insertar.
 */
public record Movimiento(
        Long id,
        Long cuentaId,
        TipoMovimiento tipo,
        BigDecimal monto,
        String descripcion,
        LocalDateTime fecha
) {

    /** Factoria para un movimiento nuevo (aun sin id, la BD lo asigna). */
    public static Movimiento nuevo(Long cuentaId, TipoMovimiento tipo,
                                   BigDecimal monto, String descripcion) {
        return new Movimiento(null, cuentaId, tipo, monto, descripcion, LocalDateTime.now());
    }
}
