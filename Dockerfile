FROM maven:3-eclipse-temurin-25@sha256:9b5f82b7166db6112eb123ba310a9e010ec3fe95022ac0728f491121563dfbab AS build
WORKDIR /build
COPY pom.xml ./
COPY src/main/ src/main/
RUN mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true clean package

FROM eclipse-temurin:25-jre@sha256:dbb43d81099eec91e0800226ed2296d03146ee7857165b95112372ad0d67e87f
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid 10001 app
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/courier-service-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
