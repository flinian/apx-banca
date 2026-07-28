package com.practica.apx.connector.jdbc;

import com.practica.apx.domain.Movimiento;
import com.practica.apx.domain.TipoMovimiento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementacion JDBC del connector de movimientos.
 */
@Repository
public class MovimientoJdbcConnectorImpl implements MovimientoJdbcConnector {

    private final JdbcTemplate jdbcTemplate;

    public MovimientoJdbcConnectorImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SQL_INSERTAR = """
            INSERT INTO MOVIMIENTO (cuenta_id, tipo, monto, descripcion, fecha)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SQL_POR_CUENTA = """
            SELECT id, cuenta_id, tipo, monto, descripcion, fecha
            FROM MOVIMIENTO
            WHERE cuenta_id = ?
            ORDER BY fecha DESC, id DESC
            """;

    private static final RowMapper<Movimiento> MOVIMIENTO_ROW_MAPPER = (rs, rowNum) -> new Movimiento(
            rs.getLong("id"),
            rs.getLong("cuenta_id"),
            TipoMovimiento.valueOf(rs.getString("tipo")),
            rs.getBigDecimal("monto"),
            rs.getString("descripcion"),
            rs.getTimestamp("fecha").toLocalDateTime()
    );

    @Override
    public void insertar(Movimiento movimiento) {
        jdbcTemplate.update(SQL_INSERTAR,
                movimiento.cuentaId(),
                movimiento.tipo().name(),
                movimiento.monto(),
                movimiento.descripcion(),
                movimiento.fecha());
    }

    @Override
    public List<Movimiento> buscarPorCuentaId(Long cuentaId) {
        return jdbcTemplate.query(SQL_POR_CUENTA, MOVIMIENTO_ROW_MAPPER, cuentaId);
    }
}
