package com.practica.apx.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuracion de clientes HTTP salientes (API Connectors).
 *
 * @Configuration: marca esta clase como fuente de definiciones de beans.
 * @Bean: cada metodo asi anotado PRODUCE un bean; lo que devuelve queda
 * registrado en el contenedor de Spring y se puede inyectar en otras clases.
 *
 * Usamos esto (y no @Component) porque RestClient es una clase de Spring: no
 * es nuestra, no le podemos poner una anotacion encima, asi que la fabricamos
 * nosotros en un metodo factoria.
 */
@Configuration
public class RestClientConfig {

    /**
     * URL base de la API externa de tipo de cambio.
     * @Value inyecta el valor de la propiedad definida en application.yml.
     * Externalizar la URL permite cambiarla por entorno (dev/prod) sin recompilar.
     */
    @Value("${app.externo.tipo-cambio.base-url}")
    private String tipoCambioBaseUrl;

    @Bean
    public RestClient tipoCambioRestClient() {
        // El builder configura el cliente una sola vez (base URL, cabeceras,
        // timeouts...). Aqui fijamos la URL base; las rutas concretas se anaden
        // en cada llamada dentro del connector.
        return RestClient.builder()
                .baseUrl(tipoCambioBaseUrl)
                .build();
    }
}
