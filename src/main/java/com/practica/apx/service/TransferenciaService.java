package com.practica.apx.service;

import com.practica.apx.dto.TransferenciaRequest;
import com.practica.apx.dto.TransferenciaResponse;

/**
 * Contrato de la logica de negocio de transferencias.
 */
public interface TransferenciaService {

    /**
     * Ejecuta una transferencia entre dos cuentas aplicando las reglas de
     * negocio (existencia, estado, moneda, saldo) de forma atomica.
     *
     * @throws com.practica.apx.exception.RecursoNoEncontradoException si alguna cuenta no existe
     * @throws com.practica.apx.exception.ReglaNegocioException        si el negocio rechaza la operacion
     */
    TransferenciaResponse transferir(TransferenciaRequest request);
}
