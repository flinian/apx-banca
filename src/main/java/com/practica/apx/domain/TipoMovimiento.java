package com.practica.apx.domain;

/**
 * Tipos de movimiento contable sobre una cuenta.
 */
public enum TipoMovimiento {
    /** Salida de dinero de la cuenta. */
    CARGO,
    /** Entrada de dinero a la cuenta. */
    ABONO,
    /** Abono generado por la liquidacion batch de intereses. */
    INTERES
}
