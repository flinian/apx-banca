package com.practica.apx.service;

import com.practica.apx.connector.jdbc.CuentaJdbcConnector;
import com.practica.apx.connector.jdbc.MovimientoJdbcConnector;
import com.practica.apx.domain.Movimiento;
import com.practica.apx.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Logica de negocio de movimientos: valida que la cuenta exista antes de
 * consultar el historial (asi un id inexistente devuelve 404, no lista vacia).
 */
@Service
public class MovimientoServiceImpl implements MovimientoService {

    private final CuentaJdbcConnector cuentaJdbcConnector;
    private final MovimientoJdbcConnector movimientoJdbcConnector;

    public MovimientoServiceImpl(CuentaJdbcConnector cuentaJdbcConnector,
                                 MovimientoJdbcConnector movimientoJdbcConnector) {
        this.cuentaJdbcConnector = cuentaJdbcConnector;
        this.movimientoJdbcConnector = movimientoJdbcConnector;
    }

    @Override
    public List<Movimiento> consultarMovimientos(Long cuentaId) {
        cuentaJdbcConnector.buscarPorId(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la cuenta con id " + cuentaId));

        return movimientoJdbcConnector.buscarPorCuentaId(cuentaId);
    }
}
