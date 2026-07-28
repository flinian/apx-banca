package com.practica.apx.controller;

import com.practica.apx.domain.Cuenta;
import com.practica.apx.domain.CuentaValorizada;
import com.practica.apx.service.CuentaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Capa online: expone la API REST de cuentas.
 *
 * @RestController = @Controller + @ResponseBody. Registra la clase como bean web
 * y hace que lo que devuelvan sus metodos se serialice directo al cuerpo de la
 * respuesta como JSON (usando Jackson, que viene con spring-boot-starter-web).
 *
 * El Controller SOLO traduce HTTP <-> Java y delega en el Service. No lleva
 * logica de negocio ni SQL: eso rompe la separacion de capas de APX.
 */
@RestController
public class CuentaController {

    private final CuentaService cuentaService;

    // Inyeccion por constructor: Spring pasa la implementacion de CuentaService.
    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    /**
     * GET /clientes/{clienteId}/cuentas
     *
     * @GetMapping mapea peticiones HTTP GET a esta URL.
     * @PathVariable extrae el segmento {clienteId} de la ruta y lo convierte a Long.
     *
     * La URL sigue el estilo REST: los recursos se anidan (las "cuentas" de un
     * "cliente"), no se usan verbos en la ruta.
     */
    @GetMapping("/clientes/{clienteId}/cuentas")
    public List<Cuenta> obtenerCuentas(@PathVariable Long clienteId) {
        return cuentaService.consultarCuentasDeCliente(clienteId);
    }

    /**
     * GET /clientes/{clienteId}/cuentas/valorizadas?moneda=USD
     *
     * Devuelve las cuentas del cliente con el saldo convertido a la moneda
     * indicada. Combina JDBC Connector (cuentas) + API Connector (tasa).
     *
     * moneda se recibe como @RequestParam con valor por defecto "USD".
     */
    @GetMapping("/clientes/{clienteId}/cuentas/valorizadas")
    public List<CuentaValorizada> obtenerCuentasValorizadas(
            @PathVariable Long clienteId,
            @RequestParam(defaultValue = "USD") String moneda) {
        return cuentaService.consultarCuentasValorizadas(clienteId, moneda);
    }
}
