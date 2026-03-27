# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the parent pom
COPY pom.xml .

# Copy ALL module poms so Maven can resolve the project structure
# (Maven needs to see the directories listed in the <modules> section)
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY user-kyc-service/pom.xml user-kyc-service/
COPY wallet-service/pom.xml wallet-service/
COPY config-server/pom.xml config-server/
COPY eureka-server/pom.xml eureka-server/
COPY notification-service/pom.xml notification-service/
COPY integration-service/pom.xml integration-service/
COPY rewards-service/pom.xml rewards-service/
COPY admin-service/pom.xml admin-service/
COPY transaction-service/pom.xml transaction-service/

# Download dependencies for the target service
RUN mvn dependency:go-offline -pl user-kyc-service -am

# Copy source code and build
COPY user-kyc-service/src user-kyc-service/src
RUN mvn package -DskipTests -pl user-kyc-service -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/user-kyc-service/target/*.jar app.jar
EXPOSE 8064
ENTRYPOINT ["java", "-jar", "app.jar"]
