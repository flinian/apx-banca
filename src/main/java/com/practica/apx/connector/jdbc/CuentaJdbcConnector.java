package com.practica.apx.connector.jdbc;

import com.practica.apx.domain.Cuenta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Contrato del JDBC Connector para cuentas (principios #1 y #2 de APX).
 *
 * La capa de servicio dependera de ESTA interfaz, nunca de la implementacion
 * concreta que usa JDBC. Asi, el "como" (JDBC, un cliente de mainframe, otro
 * motor de BD...) puede cambiar sin afectar a la logica de negocio.
 */
public interface CuentaJdbcConnector {

    /**
     * Devuelve todas las cuentas de un cliente.
     *
     * @param clienteId identificador del cliente
     * @return lista de cuentas (vacia si el cliente no tiene ninguna)
     */
    List<Cuenta> buscarPorClienteId(Long clienteId);

    /**
     * Busca una cuenta por su id.
     * Optional expresa en el TIPO que puede no existir: obliga al llamador a
     * decidir que hacer en ese caso (en vez de arriesgarse a un null).
     */
    Optional<Cuenta> buscarPorId(Long cuentaId);

    /**
     * Busca una cuenta por su numero (el identificador que viaja en archivos
     * y canales externos, donde el id interno del backend no se conoce).
     */
    Optional<Cuenta> buscarPorNumero(String numeroCuenta);

    /**
     * Actualiza el saldo de una cuenta en el backend.
     *
     * @return numero de filas afectadas (0 si la cuenta no existe)
     */
    int actualizarSaldo(Long cuentaId, BigDecimal nuevoSaldo);
}
