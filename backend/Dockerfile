# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/target/dementia-care-0.0.1-SNAPSHOT.war app.war
EXPOSE 8080
CMD ["java", "-jar", "app.war"]
