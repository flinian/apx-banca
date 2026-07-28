package com.practica.apx.service;

import com.practica.apx.domain.Movimiento;

import java.util.List;

/**
 * Contrato de la logica de negocio de movimientos.
 */
public interface MovimientoService {

    /**
     * Devuelve el historial de movimientos de una cuenta.
     *
     * @throws com.practica.apx.exception.RecursoNoEncontradoException si la cuenta no existe
     */
    List<Movimiento> consultarMovimientos(Long cuentaId);
}
