package com.practica.apx.controller;

import com.practica.apx.dto.TransferenciaRequest;
import com.practica.apx.dto.TransferenciaResponse;
import com.practica.apx.service.TransferenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API online de transferencias.
 */
@RestController
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    /**
     * POST /transferencias
     *
     * @RequestBody: deserializa el JSON del cuerpo al record TransferenciaRequest.
     * @Valid: dispara Bean Validation sobre el DTO (400 automatico si falla).
     * @ResponseStatus(CREATED): 201, el codigo correcto al crear una operacion.
     */
    @PostMapping("/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferenciaResponse transferir(@Valid @RequestBody TransferenciaRequest request) {
        return transferenciaService.transferir(request);
    }
}
