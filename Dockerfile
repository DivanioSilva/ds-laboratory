FROM maven:3.9.14-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY application/pom.xml application/pom.xml
COPY kc-extensions/pom.xml kc-extensions/pom.xml
RUN mvn dependency:go-offline

COPY application/src application/src
COPY kc-extensions/src kc-extensions/src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build --chown=spring:spring /workspace/application/target/ds-laboratory-application-0.0.1-SNAPSHOT.jar app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
