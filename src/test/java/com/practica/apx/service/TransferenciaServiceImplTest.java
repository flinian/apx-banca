package com.practica.apx.service;

import com.practica.apx.connector.jdbc.CuentaJdbcConnector;
import com.practica.apx.connector.jdbc.MovimientoJdbcConnector;
import com.practica.apx.connector.kafka.TransferenciaEventPublisher;
import com.practica.apx.domain.Cuenta;
import com.practica.apx.dto.TransferenciaRequest;
import com.practica.apx.dto.TransferenciaResponse;
import com.practica.apx.exception.RecursoNoEncontradoException;
import com.practica.apx.exception.ReglaNegocioException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de las reglas de negocio de transferencias.
 *
 * Los connectors se MOCKEAN (Mockito): probamos la logica del Service aislada
 * de la BD y de Kafka. Esto es posible precisamente porque el Service depende
 * de interfaces (el desacople de APX tambien hace el codigo testeable).
 */
@ExtendWith(MockitoExtension.class)
class TransferenciaServiceImplTest {

    @Mock
    private CuentaJdbcConnector cuentaJdbcConnector;

    @Mock
    private MovimientoJdbcConnector movimientoJdbcConnector;

    @Mock
    private TransferenciaEventPublisher eventPublisher;

    private TransferenciaServiceImpl service;

    // Datos base reutilizados en los tests
    private final Cuenta origen = new Cuenta(
            100L, 1L, "0011-2233-4455", "AHORRO",
            new BigDecimal("1500.75"), "PEN", "ACTIVA");

    private final Cuenta destino = new Cuenta(
            200L, 2L, "0022-3344-5566", "AHORRO",
            new BigDecimal("8750.50"), "PEN", "ACTIVA");

    @BeforeEach
    void setUp() {
        service = new TransferenciaServiceImpl(
                cuentaJdbcConnector, movimientoJdbcConnector,
                eventPublisher, new SimpleMeterRegistry());
    }

    private TransferenciaRequest request(BigDecimal monto) {
        return new TransferenciaRequest(100L, 200L, monto, "pago alquiler");
    }

    @Test
    @DisplayName("transferencia valida: descuenta origen, abona destino, registra 2 movimientos y publica evento")
    void transferenciaValida() {
        when(cuentaJdbcConnector.buscarPorId(100L)).thenReturn(Optional.of(origen));
        when(cuentaJdbcConnector.buscarPorId(200L)).thenReturn(Optional.of(destino));

        TransferenciaResponse respuesta = service.transferir(request(new BigDecimal("500.00")));

        assertThat(respuesta.saldoRestanteOrigen()).isEqualByComparingTo("1000.75");
        assertThat(respuesta.moneda()).isEqualTo("PEN");

        verify(cuentaJdbcConnector).actualizarSaldo(eq(100L), eq(new BigDecimal("1000.75")));
        verify(cuentaJdbcConnector).actualizarSaldo(eq(200L), eq(new BigDecimal("9250.50")));
        verify(movimientoJdbcConnector, times(2)).insertar(any());
        verify(eventPublisher).publicar(any());
    }

    @Test
    @DisplayName("saldo insuficiente: rechaza con codigo SALDO_INSUFICIENTE y no toca la BD")
    void saldoInsuficiente() {
        when(cuentaJdbcConnector.buscarPorId(100L)).thenReturn(Optional.of(origen));
        when(cuentaJdbcConnector.buscarPorId(200L)).thenReturn(Optional.of(destino));

        assertThatThrownBy(() -> service.transferir(request(new BigDecimal("99999.00"))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasFieldOrPropertyWithValue("codigo", "SALDO_INSUFICIENTE");

        verify(cuentaJdbcConnector, never()).actualizarSaldo(any(), any());
        verify(eventPublisher, never()).publicar(any());
    }

    @Test
    @DisplayName("cuenta origen bloqueada: rechaza con CUENTA_ORIGEN_NO_ACTIVA")
    void cuentaOrigenBloqueada() {
        Cuenta bloqueada = new Cuenta(100L, 1L, "0011-2233-0000", "AHORRO",
                new BigDecimal("100.00"), "PEN", "BLOQUEADA");
        when(cuentaJdbcConnector.buscarPorId(100L)).thenReturn(Optional.of(bloqueada));
        when(cuentaJdbcConnector.buscarPorId(200L)).thenReturn(Optional.of(destino));

        assertThatThrownBy(() -> service.transferir(request(new BigDecimal("50.00"))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasFieldOrPropertyWithValue("codigo", "CUENTA_ORIGEN_NO_ACTIVA");
    }

    @Test
    @DisplayName("monedas distintas: rechaza con MONEDAS_DISTINTAS")
    void monedasDistintas() {
        Cuenta destinoUsd = new Cuenta(200L, 2L, "0022-3344-5566", "CORRIENTE",
                new BigDecimal("100.00"), "USD", "ACTIVA");
        when(cuentaJdbcConnector.buscarPorId(100L)).thenReturn(Optional.of(origen));
        when(cuentaJdbcConnector.buscarPorId(200L)).thenReturn(Optional.of(destinoUsd));

        assertThatThrownBy(() -> service.transferir(request(new BigDecimal("50.00"))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasFieldOrPropertyWithValue("codigo", "MONEDAS_DISTINTAS");
    }

    @Test
    @DisplayName("misma cuenta origen y destino: rechaza con MISMA_CUENTA")
    void mismaCuenta() {
        when(cuentaJdbcConnector.buscarPorId(100L)).thenReturn(Optional.of(origen));

        TransferenciaRequest req = new TransferenciaRequest(
                100L, 100L, new BigDecimal("50.00"), "a mi mismo");

        assertThatThrownBy(() -> service.transferir(req))
                .isInstanceOf(ReglaNegocioException.class)
                .hasFieldOrPropertyWithValue("codigo", "MISMA_CUENTA");
    }

    @Test
    @DisplayName("cuenta inexistente: lanza RecursoNoEncontradoException (404)")
    void cuentaInexistente() {
        when(cuentaJdbcConnector.buscarPorId(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transferir(request(new BigDecimal("50.00"))))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("origen");
    }
}
