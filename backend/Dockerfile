# ---------- Build: Maven + JDK 25 ----------
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

# Dependências em camada separada para aproveitar o cache do Docker.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Runtime: JRE 25 ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S flowablelab && adduser -S flowablelab -G flowablelab
COPY --from=build /workspace/target/flowablelab.jar app.jar
USER flowablelab

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
