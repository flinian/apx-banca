package com.practica.apx.service;

import com.practica.apx.domain.Cuenta;
import com.practica.apx.domain.CuentaValorizada;

import java.util.List;

/**
 * Contrato de la capa de logica de negocio para cuentas.
 *
 * El Controller (Paso 3.4) dependera de ESTA interfaz, no de la implementacion.
 * Asi el "que expongo por HTTP" queda desacoplado del "que hace la logica".
 */
public interface CuentaService {

    /**
     * Consulta las cuentas de un cliente aplicando las reglas de negocio.
     *
     * @param clienteId identificador del cliente (debe ser un id valido)
     * @return lista de cuentas del cliente (vacia si no tiene)
     */
    List<Cuenta> consultarCuentasDeCliente(Long clienteId);

    /**
     * Consulta las cuentas de un cliente y convierte cada saldo a la moneda
     * indicada, usando el tipo de cambio de la API externa.
     *
     * @param clienteId identificador del cliente
     * @param moneda    moneda destino a la que convertir los saldos (ej: "USD")
     * @return lista de cuentas valorizadas en la moneda destino
     */
    List<CuentaValorizada> consultarCuentasValorizadas(Long clienteId, String moneda);
}
