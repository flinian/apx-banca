package com.practica.apx.domain;

import java.math.BigDecimal;

/**
 * Objeto de dominio: una cuenta bancaria.
 *
 * Es un 'record' (Java 17): una clase inmutable, ideal para transportar datos.
 * El compilador genera solos el constructor, los getters (ej: cuenta.saldo()),
 * equals(), hashCode() y toString(). Menos codigo repetitivo (boilerplate).
 *
 * Usamos BigDecimal para el saldo (NO double): en dinero, double introduce
 * errores de redondeo. BigDecimal es exacto. Regla de oro en banca.
 */
public record Cuenta(
        Long id,
        Long clienteId,
        String numeroCuenta,
        String tipo,
        BigDecimal saldo,
        String moneda,
        String estado
) {
}
