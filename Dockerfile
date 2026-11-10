# Etapa 1: Compilacion con Maven y JDK 21 (compatible con release 17)
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn -B dependency:go-offline

COPY src src
RUN mvn -B clean package -DskipTests

# Etapa 2: Imagen ligera de ejecucion JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S vetapp && adduser -S vetapp -G vetapp
RUN mkdir -p /app/data && chown -R vetapp:vetapp /app

COPY --from=build /app/target/*.jar app.jar
USER vetapp

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
