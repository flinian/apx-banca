package com.practica.apx.domain;

import java.math.BigDecimal;

/**
 * Vista enriquecida de una cuenta: su saldo original MAS el saldo convertido
 * a una moneda destino, usando el tipo de cambio de la API externa.
 *
 * Es un objeto de RESULTADO de un caso de uso (combina datos de dos fuentes),
 * distinto de Cuenta que refleja la fila cruda del "mainframe".
 */
public record CuentaValorizada(
        String numeroCuenta,
        String tipo,
        BigDecimal saldoOriginal,
        String monedaOriginal,
        BigDecimal saldoConvertido,
        String monedaDestino,
        BigDecimal tasaAplicada
) {
}
