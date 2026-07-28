package com.practica.apx.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Disparadores HTTP de los jobs batch (demo y operacion manual).
 *
 * En produccion correrian programados desde un orquestador (Control-M, cron,
 * Kubernetes CronJob...), pero exponer un trigger permite relanzarlos bajo
 * demanda y demostrarlos facilmente.
 */
@RestController
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job liquidacionInteresesJob;
    private final Job abonoPlanillaJob;

    // @Qualifier: hay DOS beans de tipo Job en el contexto; el qualifier le
    // dice a Spring CUAL inyectar en cada parametro (desambiguacion por nombre).
    public BatchController(JobLauncher jobLauncher,
                           @Qualifier("liquidacionInteresesJob") Job liquidacionInteresesJob,
                           @Qualifier("abonoPlanillaJob") Job abonoPlanillaJob) {
        this.jobLauncher = jobLauncher;
        this.liquidacionInteresesJob = liquidacionInteresesJob;
        this.abonoPlanillaJob = abonoPlanillaJob;
    }

    @PostMapping("/batch/liquidacion-intereses")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> lanzarLiquidacion() throws Exception {
        // Parametro unico (timestamp): Spring Batch identifica cada ejecucion
        // por sus parametros; sin esto, un segundo lanzamiento seria rechazado
        // como "ya ejecutado".
        JobExecution ejecucion = jobLauncher.run(
                liquidacionInteresesJob,
                new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters());

        return resumen(ejecucion, "liquidacionInteresesJob");
    }

    /**
     * POST /batch/abono-planilla?archivo=data/planilla-demo.csv
     *
     * La ruta del archivo viaja como JOB PARAMETER hasta el reader (@StepScope):
     * cada ejecucion procesa su propio archivo, como en una planilla real.
     */
    @PostMapping("/batch/abono-planilla")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> lanzarAbonoPlanilla(
            @RequestParam(defaultValue = "data/planilla-demo.csv") String archivo)
            throws Exception {

        JobExecution ejecucion = jobLauncher.run(
                abonoPlanillaJob,
                new JobParametersBuilder()
                        .addString("archivo", archivo)
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters());

        return resumen(ejecucion, "abonoPlanillaJob");
    }

    /** Resumen operativo de la ejecucion: leidos, escritos y saltados por step. */
    private Map<String, Object> resumen(JobExecution ejecucion, String nombreJob) {
        StepExecution step = ejecucion.getStepExecutions().iterator().next();
        return Map.of(
                "job", nombreJob,
                "ejecucionId", ejecucion.getId(),
                "estado", ejecucion.getStatus().toString(),
                "leidos", step.getReadCount(),
                "procesados", step.getWriteCount(),
                "rechazados", step.getSkipCount());
    }
}
