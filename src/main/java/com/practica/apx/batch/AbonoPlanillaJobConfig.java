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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;

/**
 * Segundo job batch: ABONO DE PLANILLA (pago de haberes) desde archivo plano.
 *
 * Es EL escenario batch clasico de la banca: una empresa entrega un archivo
 * con las cuentas de sus empleados y los montos a abonar; un proceso nocturno
 * lo aplica masivamente. Patron completo:
 *
 *   READER    -> FlatFileItemReader: lee y parsea el CSV linea a linea
 *                (la ruta llega como JOB PARAMETER: cada ejecucion, su archivo).
 *   PROCESSOR -> valida contra el "mainframe": la cuenta debe existir y estar
 *                ACTIVA; si no, lanza AbonoRechazadoException.
 *   WRITER    -> abona el saldo y registra el movimiento contable.
 *
 * FAULT TOLERANCE: .faultTolerant().skip(...) hace que un registro invalido se
 * SALTE (queda contabilizado como skip) y el resto del archivo se procese.
 * En un proceso masivo real, abortar todo por una fila corrupta es inaceptable.
 */
@Configuration
public class AbonoPlanillaJobConfig {

    private static final Logger log = LoggerFactory.getLogger(AbonoPlanillaJobConfig.class);

    private static final int CHUNK_SIZE = 10;
    /** Maximo de registros invalidos tolerados antes de abortar el job. */
    private static final int SKIP_LIMIT = 20;

    /** Linea cruda del archivo de planilla. */
    public record LineaPlanilla(String numeroCuenta, BigDecimal monto, String referencia) {
    }

    /** Linea ya validada contra el backend: cuenta real + monto a abonar. */
    public record AbonoValidado(Cuenta cuenta, BigDecimal monto, String referencia) {
    }

    @Bean
    public Job abonoPlanillaJob(JobRepository jobRepository, Step abonarPlanillaStep) {
        return new JobBuilder("abonoPlanillaJob", jobRepository)
                .start(abonarPlanillaStep)
                .build();
    }

    @Bean
    public Step abonarPlanillaStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   FlatFileItemReader<LineaPlanilla> planillaReader,
                                   ItemProcessor<LineaPlanilla, AbonoValidado> abonoProcessor,
                                   ItemWriter<AbonoValidado> abonoWriter) {
        return new StepBuilder("abonarPlanillaStep", jobRepository)
                .<LineaPlanilla, AbonoValidado>chunk(CHUNK_SIZE, transactionManager)
                .reader(planillaReader)
                .processor(abonoProcessor)
                .writer(abonoWriter)
                // --- Tolerancia a fallos: saltar registros invalidos ---
                .faultTolerant()
                .skip(AbonoRechazadoException.class)
                .skipLimit(SKIP_LIMIT)
                .listener(new org.springframework.batch.core.SkipListener<LineaPlanilla, AbonoValidado>() {
                    @Override
                    public void onSkipInProcess(LineaPlanilla linea, Throwable t) {
                        // Cada salto queda trazado: en un banco real esto iria
                        // ademas a un archivo/tabla de rechazos para reproceso.
                        log.warn("[PLANILLA][RECHAZADO] {} -> {}", linea, t.getMessage());
                    }
                })
                .build();
    }

    /**
     * @StepScope: el bean se crea EN CADA EJECUCION del step, lo que permite
     * inyectar job parameters con SpEL (#{jobParameters[...]}). Sin esto, la
     * ruta del archivo quedaria fijada al arrancar la aplicacion.
     */
    @Bean
    @StepScope
    public FlatFileItemReader<LineaPlanilla> planillaReader(
            @Value("#{jobParameters['archivo']}") String rutaArchivo) {
        return new FlatFileItemReaderBuilder<LineaPlanilla>()
                .name("planillaReader")
                .resource(new FileSystemResource(rutaArchivo))
                .linesToSkip(1)                       // cabecera del CSV
                .delimited()
                .delimiter(";")
                .names("numeroCuenta", "monto", "referencia")
                .fieldSetMapper(fieldSet -> new LineaPlanilla(
                        fieldSet.readString("numeroCuenta").trim(),
                        fieldSet.readBigDecimal("monto"),
                        fieldSet.readString("referencia").trim()))
                .build();
    }

    @Bean
    public ItemProcessor<LineaPlanilla, AbonoValidado> abonoProcessor(
            CuentaJdbcConnector cuentaJdbcConnector) {
        return linea -> {
            if (linea.monto() == null || linea.monto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AbonoRechazadoException(
                        "Monto invalido: " + linea.monto());
            }

            Cuenta cuenta = cuentaJdbcConnector.buscarPorNumero(linea.numeroCuenta())
                    .orElseThrow(() -> new AbonoRechazadoException(
                            "Cuenta inexistente: " + linea.numeroCuenta()));

            if (!"ACTIVA".equals(cuenta.estado())) {
                throw new AbonoRechazadoException(
                        "Cuenta " + cuenta.numeroCuenta() + " esta " + cuenta.estado());
            }

            return new AbonoValidado(cuenta, linea.monto(), linea.referencia());
        };
    }

    @Bean
    public ItemWriter<AbonoValidado> abonoWriter(CuentaJdbcConnector cuentaJdbcConnector,
                                                 MovimientoJdbcConnector movimientoJdbcConnector) {
        return chunk -> {
            for (AbonoValidado abono : chunk) {
                BigDecimal nuevoSaldo = abono.cuenta().saldo().add(abono.monto());

                cuentaJdbcConnector.actualizarSaldo(abono.cuenta().id(), nuevoSaldo);
                movimientoJdbcConnector.insertar(Movimiento.nuevo(
                        abono.cuenta().id(), TipoMovimiento.ABONO, abono.monto(),
                        "Abono planilla: " + abono.referencia()));

                log.info("[PLANILLA] Abonados {} {} a {}",
                        abono.monto(), abono.cuenta().moneda(), abono.cuenta().numeroCuenta());
            }
        };
    }
}
