FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM rabbitmq:3-alpine
USER root
RUN apk add --no-cache openjdk21-jre-headless ca-certificates fontconfig ttf-dejavu \
    && mkdir -p /app/dados/fotos
COPY docker/rabbitmq.conf /etc/rabbitmq/rabbitmq.conf
RUN printf '%s\n' '[].' > /etc/rabbitmq/enabled_plugins
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
WORKDIR /app
COPY --from=build /app/target/pontegames-api-0.0.1-SNAPSHOT.jar app.jar
ENV RABBITMQ_HOST=127.0.0.1 \
    RABBITMQ_EMBUTIDO=true \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -XX:MaxRAMPercentage=35.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
