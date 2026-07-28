package com.practica.apx.exception;

/**
 * Se lanza cuando un recurso solicitado no existe en el backend.
 * El ApiExceptionHandler la traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
