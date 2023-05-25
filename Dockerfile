FROM openjdk:17-jdk-slim AS build
RUN apt-get install libfreetype6

WORKDIR /app
COPY .m2/settings.xml /root/.m2/settings.xml
COPY pom.xml /app/pom.xml
COPY mvnw /app/mvnw
COPY .mvn /app/.mvn

RUN ./mvnw verify clean --fail-never
COPY . /app
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jdk-slim AS runtime
RUN apt-get install libfreetype6
COPY --from=build /app/target/*.jar /app/job.jar
WORKDIR /app

ENTRYPOINT ["java", "-jar", "job.jar"]
