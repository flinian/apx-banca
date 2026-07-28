package com.practica.apx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicacion.
 *
 * @SpringBootApplication es una anotacion "combo" que activa tres cosas:
 *   - @Configuration        : esta clase puede definir beans.
 *   - @EnableAutoConfiguration: Spring configura solas muchas cosas segun las
 *                               librerias que encuentre en el classpath (ej: al
 *                               ver spring-boot-starter-web, levanta Tomcat).
 *   - @ComponentScan         : escanea este paquete (com.practica.apx) y sus
 *                               subpaquetes buscando @Component/@Service/@Repository/
 *                               @RestController para registrarlos como beans.
 *
 * Por eso la clase principal DEBE vivir en el paquete raiz: para que el escaneo
 * alcance a todas las capas (controller, service, connector...).
 */
@SpringBootApplication
public class ApxBancaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApxBancaApplication.class, args);
    }
}
