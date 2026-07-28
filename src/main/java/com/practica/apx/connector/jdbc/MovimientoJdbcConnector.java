package com.practica.apx.connector.jdbc;

import com.practica.apx.domain.Movimiento;

import java.util.List;

/**
 * Contrato del JDBC Connector para movimientos contables.
 */
public interface MovimientoJdbcConnector {

    /** Registra un movimiento en el backend. */
    void insertar(Movimiento movimiento);

    /** Devuelve los movimientos de una cuenta, del mas reciente al mas antiguo. */
    List<Movimiento> buscarPorCuentaId(Long cuentaId);
}
