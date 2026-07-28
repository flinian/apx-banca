package com.practica.apx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO de entrada para ordenar una transferencia.
 *
 * Bean Validation (jakarta.validation): las anotaciones declaran las reglas
 * sintacticas del request; Spring las evalua ANTES de entrar al controller
 * (gracias a @Valid) y responde 400 automaticamente si fallan. Las reglas de
 * NEGOCIO (saldo, estado...) van en el Service, no aqui.
 */
public record TransferenciaRequest(

        @NotNull(message = "cuentaOrigenId es obligatorio")
        Long cuentaOrigenId,

        @NotNull(message = "cuentaDestinoId es obligatorio")
        Long cuentaDestinoId,

        @NotNull(message = "monto es obligatorio")
        @Positive(message = "monto debe ser mayor que cero")
        BigDecimal monto,

        @NotBlank(message = "descripcion es obligatoria")
        String descripcion
) {
}
