package com.practica.apx.service;

import com.practica.apx.connector.api.TipoCambioConnector;
import com.practica.apx.connector.jdbc.CuentaJdbcConnector;
import com.practica.apx.domain.Cuenta;
import com.practica.apx.domain.CuentaValorizada;
import com.practica.apx.externo.TipoCambio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de la logica de cuentas valorizadas.
 */
@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaJdbcConnector cuentaJdbcConnector;

    @Mock
    private TipoCambioConnector tipoCambioConnector;

    @InjectMocks
    private CuentaServiceImpl service;

    @Test
    @DisplayName("clienteId invalido: lanza IllegalArgumentException")
    void clienteIdInvalido() {
        assertThatThrownBy(() -> service.consultarCuentasDeCliente(-5L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.consultarCuentasDeCliente(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("valorizar: convierte saldos y NO repite llamadas a la API para la misma moneda (memo)")
    void valorizaConMemo() {
        // Dos cuentas PEN y una USD; destino USD.
        List<Cuenta> cuentas = List.of(
                new Cuenta(1L, 1L, "C1", "AHORRO", new BigDecimal("100.00"), "PEN", "ACTIVA"),
                new Cuenta(2L, 1L, "C2", "AHORRO", new BigDecimal("200.00"), "PEN", "ACTIVA"),
                new Cuenta(3L, 1L, "C3", "CORRIENTE", new BigDecimal("50.00"), "USD", "ACTIVA"));

        when(cuentaJdbcConnector.buscarPorClienteId(1L)).thenReturn(cuentas);
        when(tipoCambioConnector.obtenerTipoCambio("PEN", "USD"))
                .thenReturn(new TipoCambio("PEN", "USD", new BigDecimal("0.27")));

        List<CuentaValorizada> resultado = service.consultarCuentasValorizadas(1L, "USD");

        assertThat(resultado).hasSize(3);
        // 100 * 0.27 = 27.00
        assertThat(resultado.get(0).saldoConvertido()).isEqualByComparingTo("27.00");
        // 200 * 0.27 = 54.00
        assertThat(resultado.get(1).saldoConvertido()).isEqualByComparingTo("54.00");
        // USD -> USD: tasa 1, sin llamada externa
        assertThat(resultado.get(2).saldoConvertido()).isEqualByComparingTo("50.00");
        assertThat(resultado.get(2).tasaAplicada()).isEqualByComparingTo("1");

        // La API externa se llamo UNA sola vez aunque habia dos cuentas PEN.
        verify(tipoCambioConnector, times(1)).obtenerTipoCambio("PEN", "USD");
        verify(tipoCambioConnector, times(1)).obtenerTipoCambio(anyString(), anyString());
    }
}
