package com.practica.apx.service;

import com.practica.apx.connector.jdbc.CuentaJdbcConnector;
import com.practica.apx.connector.jdbc.MovimientoJdbcConnector;
import com.practica.apx.connector.kafka.EventoTransferencia;
import com.practica.apx.connector.kafka.TransferenciaEventPublisher;
import com.practica.apx.domain.Cuenta;
import com.practica.apx.domain.Movimiento;
import com.practica.apx.domain.TipoMovimiento;
import com.practica.apx.dto.TransferenciaRequest;
import com.practica.apx.dto.TransferenciaResponse;
import com.practica.apx.exception.RecursoNoEncontradoException;
import com.practica.apx.exception.ReglaNegocioException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Logica de negocio de transferencias: el caso de uso mas rico del proyecto.
 *
 * Orquesta JDBC Connector (cuentas y movimientos) + Kafka Connector (evento)
 * y aplica las reglas del negocio bancario.
 *
 * @Transactional: todas las escrituras (2 updates de saldo + 2 movimientos)
 * ocurren en UNA transaccion. Si algo falla a mitad, Spring hace rollback y el
 * dinero ni se duplica ni desaparece. En banca esto no es opcional.
 */
@Service
public class TransferenciaServiceImpl implements TransferenciaService {

    private static final Logger log = LoggerFactory.getLogger(TransferenciaServiceImpl.class);

    private static final String ESTADO_ACTIVA = "ACTIVA";

    private final CuentaJdbcConnector cuentaJdbcConnector;
    private final MovimientoJdbcConnector movimientoJdbcConnector;
    private final TransferenciaEventPublisher eventPublisher;

    // Metrica de negocio (Micrometer): cuantas transferencias se ejecutaron y
    // cuantas fueron rechazadas. Visible en /actuator/metrics y /actuator/prometheus.
    private final Counter transferenciasOk;
    private final Counter transferenciasRechazadas;

    public TransferenciaServiceImpl(CuentaJdbcConnector cuentaJdbcConnector,
                                    MovimientoJdbcConnector movimientoJdbcConnector,
                                    TransferenciaEventPublisher eventPublisher,
                                    MeterRegistry meterRegistry) {
        this.cuentaJdbcConnector = cuentaJdbcConnector;
        this.movimientoJdbcConnector = movimientoJdbcConnector;
        this.eventPublisher = eventPublisher;
        this.transferenciasOk = meterRegistry.counter("apx.transferencias", "resultado", "ok");
        this.transferenciasRechazadas = meterRegistry.counter("apx.transferencias", "resultado", "rechazada");
    }

    @Override
    @Transactional
    public TransferenciaResponse transferir(TransferenciaRequest request) {
        log.info("Transferencia solicitada: cuenta {} -> cuenta {} por {}",
                request.cuentaOrigenId(), request.cuentaDestinoId(), request.monto());

        try {
            Cuenta origen = obtenerCuenta(request.cuentaOrigenId(), "origen");
            Cuenta destino = obtenerCuenta(request.cuentaDestinoId(), "destino");

            validarReglasDeNegocio(request, origen, destino);

            // --- Ejecucion contable (atomica gracias a @Transactional) ---
            BigDecimal nuevoSaldoOrigen = origen.saldo().subtract(request.monto());
            BigDecimal nuevoSaldoDestino = destino.saldo().add(request.monto());

            cuentaJdbcConnector.actualizarSaldo(origen.id(), nuevoSaldoOrigen);
            cuentaJdbcConnector.actualizarSaldo(destino.id(), nuevoSaldoDestino);

            movimientoJdbcConnector.insertar(Movimiento.nuevo(
                    origen.id(), TipoMovimiento.CARGO, request.monto(),
                    "Transferencia a " + destino.numeroCuenta() + ": " + request.descripcion()));
            movimientoJdbcConnector.insertar(Movimiento.nuevo(
                    destino.id(), TipoMovimiento.ABONO, request.monto(),
                    "Transferencia de " + origen.numeroCuenta() + ": " + request.descripcion()));

            // --- Evento de dominio (Kafka Connector) ---
            eventPublisher.publicar(new EventoTransferencia(
                    origen.numeroCuenta(), destino.numeroCuenta(),
                    request.monto(), origen.moneda(),
                    request.descripcion(), LocalDateTime.now()));

            transferenciasOk.increment();
            log.info("Transferencia ejecutada: {} -> {} por {} {}",
                    origen.numeroCuenta(), destino.numeroCuenta(), request.monto(), origen.moneda());

            return new TransferenciaResponse(
                    origen.numeroCuenta(), destino.numeroCuenta(),
                    request.monto(), origen.moneda(),
                    nuevoSaldoOrigen, request.descripcion(), LocalDateTime.now());

        } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
            transferenciasRechazadas.increment();
            log.warn("Transferencia rechazada: {}", e.getMessage());
            throw e; // relanzamos: el handler web decide el codigo HTTP
        }
    }

    private Cuenta obtenerCuenta(Long cuentaId, String rol) {
        return cuentaJdbcConnector.buscarPorId(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la cuenta " + rol + " con id " + cuentaId));
    }

    private void validarReglasDeNegocio(TransferenciaRequest request, Cuenta origen, Cuenta destino) {
        if (origen.id().equals(destino.id())) {
            throw new ReglaNegocioException("MISMA_CUENTA",
                    "La cuenta origen y destino no pueden ser la misma");
        }
        if (!ESTADO_ACTIVA.equals(origen.estado())) {
            throw new ReglaNegocioException("CUENTA_ORIGEN_NO_ACTIVA",
                    "La cuenta origen " + origen.numeroCuenta() + " esta " + origen.estado());
        }
        if (!ESTADO_ACTIVA.equals(destino.estado())) {
            throw new ReglaNegocioException("CUENTA_DESTINO_NO_ACTIVA",
                    "La cuenta destino " + destino.numeroCuenta() + " esta " + destino.estado());
        }
        if (!origen.moneda().equals(destino.moneda())) {
            throw new ReglaNegocioException("MONEDAS_DISTINTAS",
                    "No se puede transferir de " + origen.moneda() + " a " + destino.moneda()
                            + " (se requiere operacion de cambio)");
        }
        // compareTo (no equals) para BigDecimal: 100.0 y 100.00 deben ser iguales.
        if (origen.saldo().compareTo(request.monto()) < 0) {
            throw new ReglaNegocioException("SALDO_INSUFICIENTE",
                    "Saldo insuficiente en " + origen.numeroCuenta()
                            + ": disponible " + origen.saldo() + ", solicitado " + request.monto());
        }
    }
}
