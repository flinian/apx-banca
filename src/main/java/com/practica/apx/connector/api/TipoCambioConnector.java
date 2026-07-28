package com.practica.apx.connector.api;

import com.practica.apx.externo.TipoCambio;

/**
 * Contrato del API Connector de tipo de cambio (principios #1 y #2 de APX).
 *
 * La capa de servicio dependera de esta interfaz, no del RestClient ni de la
 * URL concreta. Si el proveedor externo cambia, solo tocamos la implementacion.
 */
public interface TipoCambioConnector {

    /**
     * Consulta el tipo de cambio entre dos monedas llamando a la API externa.
     *
     * @param origen  moneda origen (ej: "USD")
     * @param destino moneda destino (ej: "PEN")
     * @return el tipo de cambio devuelto por la API externa
     */
    TipoCambio obtenerTipoCambio(String origen, String destino);
}
