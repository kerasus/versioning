## Build
FROM repo.asredanesh.com/gradle as build
ARG CI_REGISTRY_USERNAME
ENV CI_REGISTRY_USERNAME=$CI_REGISTRY_USERNAME
ARG CI_REGISTRY_PASSWORD
ENV CI_REGISTRY_PASSWORD=$CI_REGISTRY_PASSWORD
WORKDIR /app
COPY *.gradle .
RUN gradle dependencies --refresh-dependencies
COPY src/ src/
RUN gradle clean test bootJar

## Final
FROM repo.asredanesh.com/openjdk:21-slim
ARG JAR_FILE=/app/build/libs/*.jar
COPY --from=build ${JAR_FILE} app.jar
ENV PORT=1200
EXPOSE 1200
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=docker","/app.jar"]
