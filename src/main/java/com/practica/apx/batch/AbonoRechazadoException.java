package com.practica.apx.batch;

/**
 * Un registro del archivo de planilla no puede abonarse (cuenta inexistente,
 * bloqueada...). Es una excepcion "saltable": el job la registra y CONTINUA
 * con el resto del archivo en lugar de abortar (fault tolerance).
 */
public class AbonoRechazadoException extends RuntimeException {

    public AbonoRechazadoException(String mensaje) {
        super(mensaje);
    }
}
