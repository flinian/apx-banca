package com.practica.apx.connector.jdbc;

import com.practica.apx.domain.Cuenta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion JDBC del connector de cuentas.
 *
 * @Repository: la marca como bean de la capa de datos y activa la traduccion
 * de excepciones (SQLException -> DataAccessException de Spring), lo que
 * desacopla la logica de negocio del motor de BD concreto.
 */
@Repository
public class CuentaJdbcConnectorImpl implements CuentaJdbcConnector {

    /**
     * JdbcTemplate es el corazon del connector: ejecuta SQL y gestiona por
     * nosotros el ciclo de conexion (abrir/cerrar), los PreparedStatement y
     * los recursos. Spring lo crea automaticamente porque configuramos un
     * DataSource en application.yml.
     */
    private final JdbcTemplate jdbcTemplate;

    // Inyeccion por constructor (buena practica). Un solo constructor => Spring
    // inyecta el JdbcTemplate sin necesidad de @Autowired.
    public CuentaJdbcConnectorImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * SQL con parametro '?' (placeholder). Nunca concatenamos valores en el
     * string: usar parametros evita inyeccion SQL y es mas eficiente.
     */
    private static final String SQL_POR_CLIENTE = """
            SELECT id, cliente_id, numero_cuenta, tipo, saldo, moneda, estado
            FROM CUENTA
            WHERE cliente_id = ?
            """;

    /**
     * RowMapper: convierte cada fila del ResultSet en un objeto Cuenta.
     * Este mapeo manual fila->objeto es, precisamente, lo que un connector
     * hace explicito (a diferencia de un ORM que lo esconde).
     */
    private static final RowMapper<Cuenta> CUENTA_ROW_MAPPER = (rs, rowNum) -> new Cuenta(
            rs.getLong("id"),
            rs.getLong("cliente_id"),
            rs.getString("numero_cuenta"),
            rs.getString("tipo"),
            rs.getBigDecimal("saldo"),
            rs.getString("moneda"),
            rs.getString("estado")
    );

    private static final String SQL_POR_ID = """
            SELECT id, cliente_id, numero_cuenta, tipo, saldo, moneda, estado
            FROM CUENTA
            WHERE id = ?
            """;

    private static final String SQL_POR_NUMERO = """
            SELECT id, cliente_id, numero_cuenta, tipo, saldo, moneda, estado
            FROM CUENTA
            WHERE numero_cuenta = ?
            """;

    private static final String SQL_ACTUALIZAR_SALDO = """
            UPDATE CUENTA SET saldo = ? WHERE id = ?
            """;

    @Override
    public List<Cuenta> buscarPorClienteId(Long clienteId) {
        // query(sql, rowMapper, args...) devuelve una lista ya mapeada.
        return jdbcTemplate.query(SQL_POR_CLIENTE, CUENTA_ROW_MAPPER, clienteId);
    }

    @Override
    public Optional<Cuenta> buscarPorId(Long cuentaId) {
        // query + stream().findFirst() en vez de queryForObject: este ultimo
        // lanza excepcion si no hay filas; asi devolvemos Optional.empty().
        return jdbcTemplate.query(SQL_POR_ID, CUENTA_ROW_MAPPER, cuentaId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Cuenta> buscarPorNumero(String numeroCuenta) {
        return jdbcTemplate.query(SQL_POR_NUMERO, CUENTA_ROW_MAPPER, numeroCuenta)
                .stream()
                .findFirst();
    }

    @Override
    public int actualizarSaldo(Long cuentaId, BigDecimal nuevoSaldo) {
        // update() devuelve el numero de filas afectadas.
        return jdbcTemplate.update(SQL_ACTUALIZAR_SALDO, nuevoSaldo, cuentaId);
    }
}
