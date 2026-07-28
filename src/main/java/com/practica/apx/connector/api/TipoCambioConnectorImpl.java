package com.practica.apx.connector.api;

import com.practica.apx.externo.TipoCambio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del API Connector usando RestClient (cliente HTTP de Spring 6).
 *
 * @Component: la registra como bean. Usamos @Component (no @Repository, que es
 * para BD) porque semanticamente es un conector de salida HTTP, no un DAO.
 *
 * Igual que el JDBC Connector concentraba TODO el acceso a la BD, este concentra
 * TODA la llamada a la API externa: URL, parametros, mapeo de la respuesta y
 * (mas adelante) timeouts/reintentos/errores en un unico sitio.
 */
@Component
public class TipoCambioConnectorImpl implements TipoCambioConnector {

    private static final Logger log = LoggerFactory.getLogger(TipoCambioConnectorImpl.class);

    // Inyectamos el bean RestClient que fabricamos en RestClientConfig.
    private final RestClient tipoCambioRestClient;

    public TipoCambioConnectorImpl(RestClient tipoCambioRestClient) {
        this.tipoCambioRestClient = tipoCambioRestClient;
    }

    @Override
    public TipoCambio obtenerTipoCambio(String origen, String destino) {
        log.info("Llamando API externa de tipo de cambio {}->{}", origen, destino);

        // Construccion fluida de la llamada HTTP:
        //   get()      -> metodo GET
        //   uri(...)   -> ruta relativa a la baseUrl + query params (origen/destino)
        //   retrieve() -> ejecuta la peticion
        //   body(...)  -> deserializa el JSON de respuesta a un objeto TipoCambio
        TipoCambio respuesta = tipoCambioRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/externo/tipo-cambio")
                        .queryParam("origen", origen)
                        .queryParam("destino", destino)
                        .build())
                .retrieve()
                .body(TipoCambio.class);

        log.info("Tipo de cambio {}->{} = {}", origen, destino,
                respuesta != null ? respuesta.tasa() : null);
        return respuesta;
    }
}
