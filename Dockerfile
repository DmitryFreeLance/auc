FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S auction && adduser -S auction -G auction && mkdir -p /app/data/uploads && chown -R auction:auction /app
COPY deploy/certs/russian-trusted-root-ca.pem /tmp/russian-trusted-root-ca.pem
RUN keytool -importcert -noprompt -trustcacerts -alias russian-trusted-root-ca -file /tmp/russian-trusted-root-ca.pem -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit && rm /tmp/russian-trusted-root-ca.pem
WORKDIR /app
COPY --from=build /build/target/max-auto-auction-*.jar app.jar
USER auction
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=3s --start-period=30s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
