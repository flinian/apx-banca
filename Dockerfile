# ==========================================================================
# Build multi-stage: compila con Maven y ejecuta sobre un JRE minimo.
# La imagen final no contiene Maven ni el codigo fuente, solo el jar.
# ==========================================================================

# ---- Etapa 1: build ------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Primero solo el pom: si no cambia, Docker reutiliza la capa con las
# dependencias descargadas y los rebuilds son mucho mas rapidos.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Etapa 2: runtime ----------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios: nunca ejecutar la app como root.
RUN addgroup -S apx && adduser -S apx -G apx
USER apx

COPY --from=build /workspace/target/apx-banca-*.jar app.jar

# El archivo de planilla de demo para poder probar el batch dentro del contenedor.
COPY --chown=apx:apx data ./data

EXPOSE 8080

# Healthcheck contra Actuator: el orquestador sabe si la app esta viva.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
