# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy ALL poms for multi-module resolution
COPY pom.xml .
COPY user-kyc-service/pom.xml user-kyc-service/

# Download dependencies (only for this service and its parent)
RUN mvn dependency:go-offline -pl user-kyc-service -am

# Copy source and build
COPY user-kyc-service/src user-kyc-service/src
RUN mvn package -DskipTests -pl user-kyc-service -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/user-kyc-service/target/*.jar app.jar
EXPOSE 8064
ENTRYPOINT ["java", "-jar", "app.jar"]
