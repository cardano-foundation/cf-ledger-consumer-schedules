FROM openjdk:17-jdk-slim AS build
RUN apt-get update && apt-get install -y fontconfig libfreetype6 && rm -rf /var/lib/apt/lists/*

ARG PRIVATE_MVN_REGISTRY_URL
ARG PRIVATE_MVN_REGISTRY_USER
ARG PRIVATE_MVN_REGISTRY_PASS
ARG SETTINGS_XML_TPL=.m2/settings.default.xml.tpl
WORKDIR /app
COPY ${SETTINGS_XML_TPL} /root/${SETTINGS_XML_TPL}
RUN envsubst < /root/${SETTINGS_XML_TPL} > /root/.m2/settings.xml

COPY pom.xml /app/pom.xml
COPY mvnw /app/mvnw
COPY .mvn /app/.mvn

RUN ./mvnw verify clean --fail-never
COPY . /app
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jdk-slim AS runtime
RUN apt-get update && apt-get install -y fontconfig libfreetype6 && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar /app/job.jar
WORKDIR /app

ENTRYPOINT ["java", "-jar", "job.jar"]
