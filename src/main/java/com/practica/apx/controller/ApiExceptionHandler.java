package com.practica.apx.controller;

import com.practica.apx.exception.RecursoNoEncontradoException;
import com.practica.apx.exception.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la capa web.
 *
 * @RestControllerAdvice: intercepta excepciones lanzadas por CUALQUIER
 * @RestController y las traduce a respuestas HTTP coherentes en formato
 * ProblemDetail (RFC 7807). Un unico "vocabulario de errores" para toda la
 * API: estandarizacion pura estilo APX.
 *
 * Mapa de errores:
 *   400 Bad Request          -> peticion mal formada (validacion sintactica)
 *   404 Not Found            -> el recurso no existe
 *   422 Unprocessable Entity -> peticion valida pero rechazada por el negocio
 *   500 Internal Server Error-> fallo no previsto (sin filtrar detalles internos)
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Validaciones de negocio simples (IllegalArgumentException) -> 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail manejarArgumentoInvalido(IllegalArgumentException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Peticion invalida");
        return problema;
    }

    /** Bean Validation fallida en un @Valid @RequestBody -> 400 con el detalle por campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, detalle);
        problema.setTitle("Peticion invalida");
        return problema;
    }

    /** Recurso inexistente -> 404. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Recurso no encontrado");
        return problema;
    }

    /** Regla de negocio violada -> 422, con el codigo de negocio como propiedad extra. */
    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail manejarReglaNegocio(ReglaNegocioException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problema.setTitle("Operacion rechazada por reglas de negocio");
        problema.setProperty("codigo", ex.getCodigo());
        return problema;
    }

    /** Red de seguridad: cualquier error no previsto -> 500 SIN exponer internals. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorInesperado(Exception ex) {
        // El detalle real va al log (con traceId); al cliente solo un mensaje generico.
        log.error("Error no controlado", ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error interno. Contacte a soporte con el traceId de la cabecera X-Trace-Id.");
        problema.setTitle("Error interno");
        return problema;
    }
}
