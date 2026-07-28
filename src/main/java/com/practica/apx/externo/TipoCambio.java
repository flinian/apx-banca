package com.practica.apx.externo;

import java.math.BigDecimal;

/**
 * Respuesta de la API externa de tipo de cambio.
 *
 * Representa el "contrato" de datos del servicio de terceros: dada una moneda
 * origen y una destino, cuantas unidades de destino equivale 1 de origen.
 *
 * Ej: origen=USD, destino=PEN, tasa=3.75  ->  1 USD = 3.75 PEN
 */
public record TipoCambio(
        String origen,
        String destino,
        BigDecimal tasa
) {
}
