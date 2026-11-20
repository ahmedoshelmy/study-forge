# syntax=docker/dockerfile:1
# One Dockerfile for both services: build the multi-module reactor once per image,
# pick the module with --build-arg SERVICE=learning-service|insights-service.
ARG SERVICE=learning-service

FROM eclipse-temurin:25-jdk AS build
ARG SERVICE
WORKDIR /src
ENV MAVEN_OPTS=-Xmx512m
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY contracts/pom.xml contracts/pom.xml
COPY learning-service/pom.xml learning-service/pom.xml
COPY insights-service/pom.xml insights-service/pom.xml
COPY contracts/src contracts/src
COPY ${SERVICE}/src ${SERVICE}/src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -q -pl ${SERVICE} -am package -DskipTests -Dspotless.check.skip=true \
 && java -Djarmode=tools -jar ${SERVICE}/target/${SERVICE}-*.jar extract --destination /app \
 && mv /app/${SERVICE}-*.jar /app/app.jar

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app ./
USER app
EXPOSE 8080 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
