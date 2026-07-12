# ── Stage 1: Build ─────────────────────────────────────────────────────────
# Base image with Maven 3.9 and Eclipse Temurin JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory inside the container
WORKDIR /app

# Copy the entire multi-module project into the container
COPY . .

# Build only the server module and its required dependencies (common), skip tests
RUN mvn -q -DskipTests package -pl server -am

# ── Stage 2: Run ────────────────────────────────────────────────────────────
# Lightweight JRE-only image (no build tools, smaller final image)
FROM eclipse-temurin:21-jre

# Set working directory for the runtime stage
WORKDIR /app

# Copy the executable fat-jar produced by Spring Boot Maven plugin
COPY --from=build /app/server/target/server-developer-SNAPSHOT.jar app.jar

# Document the default port (Render overrides this with the PORT environment variable)
EXPOSE 8081

# Launch the Spring Boot application; PORT env var is picked up via server.port=${PORT:8081}
ENTRYPOINT ["java", "-jar", "app.jar"]
