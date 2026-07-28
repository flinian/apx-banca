package com.practica.apx.exception;

/**
 * Se lanza cuando una operacion viola una regla de negocio (saldo insuficiente,
 * cuenta bloqueada, monedas incompatibles...). El ApiExceptionHandler la
 * traduce a HTTP 422 (Unprocessable Entity): la peticion es sintacticamente
 * valida pero el negocio la rechaza.
 */
public class ReglaNegocioException extends RuntimeException {

    /** Codigo corto y estable del error, util para que el consumidor lo trate. */
    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
