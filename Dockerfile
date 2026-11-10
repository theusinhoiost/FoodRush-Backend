# syntax=docker/dockerfile:1

# Stage 1: Build application with JDK 25
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and POM to pre-fetch dependencies (layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy application source code and compile
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal runtime image with JRE 25 and non-root execution
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create non-root system group and user
RUN groupadd -r foodrush && useradd -r -g foodrush -u 10001 foodrush

# Copy executable jar from builder stage
COPY --from=builder /build/target/*.jar /app/app.jar

# Set ownership
RUN chown -R foodrush:foodrush /app

USER foodrush

EXPOSE 8080

# Configure JVM container memory ergonomics for low cost & minimal memory footprint:
# - UseSerialGC: Minimal memory overhead for low-core/container environments
# - MaxRAM 384m & Heap 256m: Restricts total JVM RSS footprint comfortably within budget
# - ExitOnOutOfMemoryError: Ensures immediate restart on failure
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAM=384m -Xms128m -Xmx256m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
