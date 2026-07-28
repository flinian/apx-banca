package com.practica.apx.batch;

import com.practica.apx.connector.jdbc.CuentaJdbcConnector;
import com.practica.apx.connector.jdbc.MovimientoJdbcConnector;
import com.practica.apx.domain.Cuenta;
import com.practica.apx.domain.Movimiento;
import com.practica.apx.domain.TipoMovimiento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arquitectura BATCH de APX: liquidacion mensual de intereses.
 *
 * Job chunk-oriented clasico de Spring Batch:
 *   READER    -> lee del "mainframe" las cuentas AHORRO ACTIVAS (cursor JDBC,
 *                no carga todo en memoria: apto para millones de filas).
 *   PROCESSOR -> calcula el interes de cada cuenta (saldo * tasa).
 *   WRITER    -> registra el movimiento INTERES y actualiza el saldo, en
 *                chunks de 10 con transaccion por chunk (reintentable).
 *
 * A diferencia del flujo online (request/response, ms), esto procesa VOLUMEN:
 * corre programado o bajo demanda, tolera reinicios (Spring Batch guarda el
 * estado de cada ejecucion en sus tablas de metadatos BATCH_*).
 */
@Configuration
public class LiquidacionInteresesJobConfig {

    private static final Logger log = LoggerFactory.getLogger(LiquidacionInteresesJobConfig.class);

    /** Tasa de interes mensual simulada para cuentas de ahorro (0.5%). */
    private static final BigDecimal TASA_INTERES_MENSUAL = new BigDecimal("0.005");

    private static final int CHUNK_SIZE = 10;

    /** Cuenta + interes calculado: lo que viaja del processor al writer. */
    public record InteresCalculado(Cuenta cuenta, BigDecimal interes) {
    }

    @Bean
    public Job liquidacionInteresesJob(JobRepository jobRepository, Step liquidarInteresesStep) {
        return new JobBuilder("liquidacionInteresesJob", jobRepository)
                .start(liquidarInteresesStep)
                .build();
    }

    @Bean
    public Step liquidarInteresesStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      JdbcCursorItemReader<Cuenta> cuentasAhorroReader,
                                      ItemProcessor<Cuenta, InteresCalculado> interesProcessor,
                                      ItemWriter<InteresCalculado> interesWriter) {
        return new StepBuilder("liquidarInteresesStep", jobRepository)
                // <entrada, salida> del chunk + transaccion por chunk
                .<Cuenta, InteresCalculado>chunk(CHUNK_SIZE, transactionManager)
                .reader(cuentasAhorroReader)
                .processor(interesProcessor)
                .writer(interesWriter)
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Cuenta> cuentasAhorroReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Cuenta>()
                .name("cuentasAhorroReader")
                .dataSource(dataSource)
                .sql("""
                        SELECT id, cliente_id, numero_cuenta, tipo, saldo, moneda, estado
                        FROM CUENTA
                        WHERE tipo = 'AHORRO' AND estado = 'ACTIVA' AND saldo > 0
                        ORDER BY id
                        """)
                .rowMapper((rs, rowNum) -> new Cuenta(
                        rs.getLong("id"),
                        rs.getLong("cliente_id"),
                        rs.getString("numero_cuenta"),
                        rs.getString("tipo"),
                        rs.getBigDecimal("saldo"),
                        rs.getString("moneda"),
                        rs.getString("estado")))
                .build();
    }

    @Bean
    public ItemProcessor<Cuenta, InteresCalculado> interesProcessor() {
        return cuenta -> {
            BigDecimal interes = cuenta.saldo()
                    .multiply(TASA_INTERES_MENSUAL)
                    .setScale(2, RoundingMode.HALF_UP);

            // Devolver null descarta el item (skip): sin interes que abonar.
            if (interes.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return new InteresCalculado(cuenta, interes);
        };
    }

    @Bean
    public ItemWriter<InteresCalculado> interesWriter(CuentaJdbcConnector cuentaJdbcConnector,
                                                      MovimientoJdbcConnector movimientoJdbcConnector) {
        // Reutilizamos los MISMOS connectors JDBC del flujo online: la capa de
        // acceso a datos es una sola para online y batch (estandarizacion APX).
        return chunk -> {
            for (InteresCalculado item : chunk) {
                Cuenta cuenta = item.cuenta();
                BigDecimal nuevoSaldo = cuenta.saldo().add(item.interes());

                cuentaJdbcConnector.actualizarSaldo(cuenta.id(), nuevoSaldo);
                movimientoJdbcConnector.insertar(Movimiento.nuevo(
                        cuenta.id(), TipoMovimiento.INTERES, item.interes(),
                        "Liquidacion mensual de intereses (tasa " + TASA_INTERES_MENSUAL + ")"));

                log.info("[BATCH] Interes {} {} abonado a {}",
                        item.interes(), cuenta.moneda(), cuenta.numeroCuenta());
            }
        };
    }
}
