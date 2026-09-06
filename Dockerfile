# syntax=docker/dockerfile:1
#
# IAS Academy backend image
# - Java 17, Spring Boot 4.1.0, Maven wrapper (deployment guide + pom.xml)
# - JAR: target/project-0.0.1-SNAPSHOT.jar
# - Port 8080, profiles dev/prod
# - MySQL is not in this image; use docker-compose.yml for app + MySQL 8
#
# Build:
#   docker build -t ias-academy-backend .
#
# Run (prod). Properties listed in the deployment guide are supplied as env vars
# because application-prod.properties does not define JWT, mail, or CORS, and
# those are required at startup (@Value in SecurityConfig, JwtTokenProvider,
# EmailServiceImplementation).
#
#   docker run --rm -p 8080:8080 \
#     -e SPRING_PROFILES_ACTIVE=prod \
#     -e SPRING_DATASOURCE_URL=jdbc:mysql://db-host:3306/academy_db \
#     -e SPRING_DATASOURCE_USERNAME=prod_user \
#     -e SPRING_DATASOURCE_PASSWORD=secret \
#     -e APP_JWT_SECRET=your-strong-secret \
#     -e APP_JWT_ACCESS_TOKEN_EXPIRATION_MS=900000 \
#     -e APP_JWT_REFRESH_TOKEN_EXPIRATION_MS=604800000 \
#     -e SPRING_MAIL_HOST=smtp.example.com \
#     -e SPRING_MAIL_USERNAME=smtp-user \
#     -e SPRING_MAIL_PASSWORD=smtp-password \
#     -e ACADEMY_CONTACT_EMAIL=ops@example.com \
#     -e APP_CORS_ALLOWED_ORIGINS=https://yourdomain.com \
#     ias-academy-backend

# -----------------------------------------------------------------------------
# Stage 1: compile with the Maven wrapper (guide: ./mvnw clean package -DskipTests)
# Eclipse Temurin 17 is used because pom.xml and the guide specify Java 17.
# Spring Boot 4.1 docs show a Java 25 Liberica example; that does not match this
# project's Java version.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn

# Windows checkouts can drop the executable bit; the wrapper is only-script
# (no maven-wrapper.jar; see .mvn/wrapper/maven-wrapper.properties).
RUN chmod +x mvnw

# Tests are not copied: the guide skips them, and ProjectApplicationTests is a
# full @SpringBootTest that needs MySQL.
COPY src/main ./src/main

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B clean package -DskipTests \
    && java -Djarmode=tools -jar target/project-0.0.1-SNAPSHOT.jar \
         extract --layers --destination /extracted

# -----------------------------------------------------------------------------
# Stage 2: runtime JRE only, layered jar (Spring Boot 4.1 jarmode=tools)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring --no-create-home spring

WORKDIR /application

COPY --from=build --chown=spring:spring /extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /extracted/application/ ./

USER spring

# Guide: production run uses --spring.profiles.active=prod
ENV SPRING_PROFILES_ACTIVE=prod

# Jackson is configured to Asia/Kolkata in application.properties
ENV TZ=Asia/Kolkata

# Container-aware heap; override at runtime with -e JAVA_TOOL_OPTIONS=...
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

# TCP check on the documented port. /actuator/health is permitAll in
# SecurityConfig, but spring-boot-starter-actuator is not in pom.xml, so that
# URL is not used here. Temurin JRE has no curl/wget.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080"]

ENTRYPOINT ["java", "-jar", "application.jar"]
