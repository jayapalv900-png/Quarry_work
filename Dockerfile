FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package


FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build \
    /build/target/quarryworks-2.0.0.jar \
    /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]