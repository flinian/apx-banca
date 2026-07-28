package com.practica.apx.externo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * SIMULACION de una API externa de tipo de cambio.
 *
 * OJO: esto NO es parte de nuestra arquitectura APX; hace de "tercero" al que
 * llamaremos desde el API Connector (Paso 4.2). En un proyecto real, este
 * endpoint viviria en OTRO servicio/servidor. Lo montamos aqui solo para poder
 * practicar sin depender de internet.
 *
 * Usa parametros de consulta (query params): /externo/tipo-cambio?origen=USD&destino=PEN
 */
@RestController
public class TipoCambioMockController {

    // Tabla de tasas fijas simuladas, con clave "ORIGEN->DESTINO".
    private static final Map<String, BigDecimal> TASAS = Map.of(
            "USD->PEN", new BigDecimal("3.75"),
            "PEN->USD", new BigDecimal("0.27"),
            "EUR->PEN", new BigDecimal("4.05")
    );

    @GetMapping("/externo/tipo-cambio")
    public TipoCambio obtenerTipoCambio(
            @RequestParam String origen,
            @RequestParam String destino) {

        BigDecimal tasa = TASAS.getOrDefault(
                origen + "->" + destino,
                BigDecimal.ONE); // si no la conocemos, 1:1 (simplificacion)

        return new TipoCambio(origen, destino, tasa);
    }
}
