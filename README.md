# 🏦 apx-banca — Microservicio bancario inspirado en la arquitectura APX (BBVA)

![CI](https://github.com/USUARIO/apx-banca/actions/workflows/ci.yml/badge.svg)

Proyecto de práctica que replica la **filosofía y patrones de APX** (Arquitectura
Plataforma Extendida de BBVA) usando **Spring Boot 3 puro y librerías open source**,
sobre un dominio bancario realista: cuentas, movimientos, transferencias,
liquidación de intereses y eventos de negocio.

> APX es propietario de BBVA; este proyecto no usa sus librerías. Reproduce sus
> **principios arquitectónicos** con equivalentes estándar del ecosistema Spring.

---

## 🎯 Principios APX replicados

| Principio APX | Implementación en este proyecto |
|---|---|
| Separación estricta en capas con contratos | `controller → service → connector`, cada capa habla con la siguiente **solo vía interfaces** |
| **JDBC Connector** (acceso a datos estandarizado) | `JdbcTemplate` + `RowMapper` explícitos (`connector/jdbc`) |
| **API Connector** (invocación REST estandarizada) | `RestClient` de Spring 6 con URL externalizada (`connector/api`) |
| **Kafka Connector** (eventos pub/sub) | `spring-kafka`: productor + consumidor de auditoría (`connector/kafka`) |
| Desacople del *mainframe* legacy | H2 en memoria simula el backend; el código nunca asume qué hay detrás |
| Arquitectura **online** vs **batch** | API REST síncrona + job **Spring Batch** chunk-oriented reutilizando los mismos connectors |
| Trazabilidad y monitorización | `traceId` (MDC) en cada línea de log, Actuator, Micrometer + Prometheus, métricas de negocio |
| Configuración por entorno | `application.yml` + **profiles** (la app arranca con o sin broker Kafka) |

## 🗂️ Estructura

```
com.practica.apx
├── controller       API REST + manejo global de errores (ProblemDetail RFC 7807)
├── service          Lógica de negocio (interfaces + impl, @Transactional, métricas)
├── connector
│   ├── jdbc         JDBC Connector → H2 ("mainframe" simulado)
│   ├── api          API Connector  → API externa de tipo de cambio (RestClient)
│   └── kafka        Kafka Connector → evento TransferenciaRealizada + listener auditoría
├── batch            Job de liquidación de intereses (reader → processor → writer)
├── domain           Modelo de negocio (records inmutables, BigDecimal para dinero)
├── dto              Contratos de entrada/salida con Bean Validation
├── exception        Excepciones de negocio tipadas (404 / 422)
├── config           RestClient bean, filtro de trazabilidad (X-Trace-Id → MDC)
└── externo          API de terceros SIMULADA (solo para demo local)
```

## 🚀 Ejecución

**Opción A — local** (requiere Java 17+ y Maven 3.8+):

```bash
mvn spring-boot:run
```

**Opción B — Docker Compose** (app + Kafka, sin instalar nada más):

```bash
docker compose up -d --build
```

La imagen es multi-stage (build Maven → JRE Alpine), corre con usuario sin
privilegios y declara un `HEALTHCHECK` contra Actuator.

**Opción C — solo el broker**, app local con el perfil `kafka`:

```bash
docker compose up -d kafka
mvn spring-boot:run -Dspring-boot.run.profiles=kafka
```

Sin el perfil `kafka`, un publisher de respaldo registra los eventos en el log:
misma interfaz, distinta implementación — el negocio no cambia.

Tests:

```bash
mvn test
```

## 📡 API

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/clientes/{id}/cuentas` | Cuentas de un cliente (JDBC Connector) |
| GET | `/clientes/{id}/cuentas/valorizadas?moneda=USD` | Cuentas con saldo convertido (JDBC + API Connector) |
| GET | `/cuentas/{id}/movimientos` | Historial contable de una cuenta |
| POST | `/transferencias` | Transferencia entre cuentas (transaccional + evento Kafka) |
| POST | `/batch/liquidacion-intereses` | Lanza el job batch de intereses |
| POST | `/batch/abono-planilla?archivo=data/planilla-demo.csv` | Procesa un archivo de planilla (nómina) |
| GET | `/actuator/health` | Salud del servicio (incluye la BD) |
| GET | `/actuator/prometheus` | Métricas en formato Prometheus |

### Ejemplo: transferencia

```bash
curl -X POST http://localhost:8080/transferencias \
  -H "Content-Type: application/json" \
  -d '{"cuentaOrigenId":200,"cuentaDestinoId":100,"monto":250.50,"descripcion":"pago alquiler"}'
```

Reglas de negocio aplicadas (todas con test unitario):

- Ambas cuentas deben existir → `404`
- Origen ≠ destino, ambas `ACTIVA`, misma moneda, saldo suficiente → `422` con
  código de negocio (`SALDO_INSUFICIENTE`, `MONEDAS_DISTINTAS`, ...)
- Monto positivo y descripción obligatoria (Bean Validation) → `400`
- Ejecución **atómica** (`@Transactional`): 2 saldos + 2 movimientos, o nada
- Publica el evento `banca.transferencias.realizadas` (clave = cuenta origen,
  preservando el orden por cuenta)

### Batch de planilla (archivo plano + fault tolerance)

El job `abonoPlanillaJob` procesa un CSV de nómina (`FlatFileItemReader`), valida
cada línea contra el backend (cuenta existente y `ACTIVA`) y abona los montos.
Los registros inválidos **se saltan sin abortar el job** (`faultTolerant().skip()`)
y quedan trazados con su motivo; la respuesta incluye el resumen operativo:

```json
{"job":"abonoPlanillaJob","estado":"COMPLETED","leidos":5,"procesados":3,"rechazados":2}
```

La ruta del archivo viaja como *job parameter* hasta el reader (`@StepScope`):
cada ejecución procesa su propio archivo, como en una planilla real.

### Trazabilidad

Toda petición lleva un `traceId` (se propaga si llega `X-Trace-Id`, se genera si no)
presente en **cada línea de log** y devuelto en la respuesta:

```
2026-07-27 INFO [apx-banca,demo-123] ... CuentaServiceImpl : Consultando cuentas del cliente 1
```

### Métricas de negocio

`apx.transferencias{resultado="ok|rechazada"}` — visibles en
`/actuator/metrics/apx.transferencias` y `/actuator/prometheus`.

## 🧠 Decisiones de diseño

- **JDBC explícito en lugar de ORM**: APX modela el acceso a datos como un
  *connector* donde la consulta y el mapeo son visibles y controlados; un ORM
  ocultaría justo lo que se quiere estandarizar.
- **`BigDecimal` para dinero** y comparación con `compareTo` (no `equals`).
- **`Optional` en los contratos** de búsqueda unitaria: el tipo obliga al
  llamador a decidir qué pasa cuando el recurso no existe.
- **Errores como contrato**: `ProblemDetail` (RFC 7807) + códigos de negocio
  estables, para que los consumidores traten los errores programáticamente.
- **Profiles para infraestructura opcional**: el mismo artefacto corre en
  cualquier entorno; solo cambia la configuración (12-factor).
- **Batch reutiliza los connectors del flujo online**: una sola capa de acceso
  a datos para las dos arquitecturas (online/batch), como exige APX.

## ⚙️ CI/CD

Cada push a `main` dispara el pipeline de GitHub Actions
([.github/workflows/ci.yml](.github/workflows/ci.yml)):

1. **Build + tests** con Maven (JDK 17, caché de dependencias).
2. **Imagen Docker**: se construye y se hace *smoke test* real — el contenedor
   arranca y debe responder `UP` en `/actuator/health` para que el commit pase.

## 🧪 Tests

- `TransferenciaServiceImplTest`: las 6 reglas de negocio (Mockito, connectors mockeados).
- `CuentaServiceImplTest`: validación de entrada y memoización de llamadas al
  API Connector (verifica que la API externa se llama **una** vez por moneda).
