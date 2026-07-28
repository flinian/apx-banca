package com.practica.apx.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Trazabilidad end-to-end (principio #5 de APX).
 *
 * Cada peticion HTTP recibe un traceId:
 *  - si el cliente ya manda X-Trace-Id (viene de otro microservicio), se REUSA
 *    para que la traza continue a traves de todo el ecosistema;
 *  - si no, se genera uno nuevo.
 *
 * El id se guarda en el MDC (Mapped Diagnostic Context) de SLF4J: un mapa
 * asociado al hilo actual que el patron de logging imprime en CADA linea de
 * log de la peticion, sin que los services tengan que pasarlo a mano.
 * Tambien se devuelve en la cabecera de respuesta para que el consumidor
 * pueda reportar "mi operacion fallo, traceId X" y soporte la localice.
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String CABECERA_TRACE = "X-Trace-Id";
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = request.getHeader(CABECERA_TRACE);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(CABECERA_TRACE, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpieza obligatoria: los hilos del servidor se REUTILIZAN entre
            // peticiones; sin esto, una peticion heredaria el traceId de otra.
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
