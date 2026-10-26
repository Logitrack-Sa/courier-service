
# Etapa 1: compilar la aplicación con Java 25 y Maven
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copiar los archivos de Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Copiar el código fuente
COPY src src

# Generar el JAR de la aplicación
RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

# Etapa 2: ejecutar la aplicación con Java 25
FROM eclipse-temurin:25-jre

WORKDIR /app

# Instalar curl y crear un usuario sin privilegios
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --no-create-home courier

# Copiar el JAR desde la etapa de compilación
COPY --from=build /app/target/*.jar /app/courier-service.jar

# Ejecutar con un usuario no root
USER 10001

# Puerto de Spring Boot
EXPOSE 8082

# Comprobar la salud real de la aplicación
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent http://localhost:8082/actuator/health || exit 1

# Iniciar la aplicación
ENTRYPOINT ["java", "-jar", "/app/courier-service.jar"]
