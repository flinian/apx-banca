package com.practica.apx.service;

import com.practica.apx.connector.api.TipoCambioConnector;
import com.practica.apx.connector.jdbc.CuentaJdbcConnector;
import com.practica.apx.domain.Cuenta;
import com.practica.apx.domain.CuentaValorizada;
import com.practica.apx.externo.TipoCambio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion de la logica de negocio de cuentas.
 *
 * @Service: la registra como bean de la capa de negocio.
 *
 * Fijate en que depende de la INTERFAZ CuentaJdbcConnector, no de su Impl.
 * Spring inyecta la implementacion concreta en tiempo de ejecucion. Este es
 * el desacople entre capas (principios #1 y #2 de APX).
 */
@Service
public class CuentaServiceImpl implements CuentaService {

    // Logger para trazabilidad (principio #5). Es estatico y final por convencion
    // SLF4J; el nombre del logger sera el de esta clase.
    private static final Logger log = LoggerFactory.getLogger(CuentaServiceImpl.class);

    private final CuentaJdbcConnector cuentaJdbcConnector;
    private final TipoCambioConnector tipoCambioConnector;

    // Inyeccion por constructor: Spring pasa los beans que implementan cada
    // connector. Un solo constructor => no hace falta @Autowired.
    public CuentaServiceImpl(CuentaJdbcConnector cuentaJdbcConnector,
                             TipoCambioConnector tipoCambioConnector) {
        this.cuentaJdbcConnector = cuentaJdbcConnector;
        this.tipoCambioConnector = tipoCambioConnector;
    }

    @Override
    public List<Cuenta> consultarCuentasDeCliente(Long clienteId) {
        // ---- Regla de negocio: validar la entrada antes de ir al backend ----
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException(
                    "El clienteId debe ser un numero positivo. Recibido: " + clienteId);
        }

        log.info("Consultando cuentas del cliente {}", clienteId);

        List<Cuenta> cuentas = cuentaJdbcConnector.buscarPorClienteId(clienteId);

        log.info("Cliente {} tiene {} cuenta(s)", clienteId, cuentas.size());
        return cuentas;
    }

    @Override
    public List<CuentaValorizada> consultarCuentasValorizadas(Long clienteId, String moneda) {
        // Reutilizamos la validacion + consulta del metodo anterior (JDBC Connector).
        List<Cuenta> cuentas = consultarCuentasDeCliente(clienteId);

        // Memo: cache local de tasas ya consultadas en ESTA peticion, para no
        // llamar a la API externa varias veces por la misma moneda origen.
        Map<String, BigDecimal> tasasPorMonedaOrigen = new HashMap<>();

        return cuentas.stream()
                .map(cuenta -> valorizar(cuenta, moneda, tasasPorMonedaOrigen))
                .toList();
    }

    /**
     * Convierte el saldo de una cuenta a la moneda destino.
     * Si origen y destino coinciden, la tasa es 1 y no llamamos a la API.
     */
    private CuentaValorizada valorizar(Cuenta cuenta, String monedaDestino,
                                       Map<String, BigDecimal> memo) {
        BigDecimal tasa;
        if (cuenta.moneda().equals(monedaDestino)) {
            tasa = BigDecimal.ONE;
        } else {
            // computeIfAbsent: si ya consultamos esta moneda origen, reusa la
            // tasa; si no, llama al API Connector y la guarda.
            tasa = memo.computeIfAbsent(cuenta.moneda(), origen -> {
                TipoCambio tc = tipoCambioConnector.obtenerTipoCambio(origen, monedaDestino);
                return tc.tasa();
            });
        }

        // saldoConvertido = saldo * tasa, redondeado a 2 decimales (dinero).
        BigDecimal saldoConvertido = cuenta.saldo()
                .multiply(tasa)
                .setScale(2, RoundingMode.HALF_UP);

        return new CuentaValorizada(
                cuenta.numeroCuenta(),
                cuenta.tipo(),
                cuenta.saldo(),
                cuenta.moneda(),
                saldoConvertido,
                monedaDestino,
                tasa
        );
    }
}
