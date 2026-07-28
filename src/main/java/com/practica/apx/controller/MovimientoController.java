package com.practica.apx.controller;

import com.practica.apx.domain.Movimiento;
import com.practica.apx.service.MovimientoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API online de movimientos (historial contable de una cuenta).
 */
@RestController
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    /** GET /cuentas/{cuentaId}/movimientos */
    @GetMapping("/cuentas/{cuentaId}/movimientos")
    public List<Movimiento> obtenerMovimientos(@PathVariable Long cuentaId) {
        return movimientoService.consultarMovimientos(cuentaId);
    }
}
