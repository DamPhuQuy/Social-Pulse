# Stage 1: Build stage using JDK 25
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy gradle files for dependency caching
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Ensure gradlew has execute permission and download dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the application jar file (skipping tests)
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime stage using JRE 25
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose standard Spring Boot port
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
