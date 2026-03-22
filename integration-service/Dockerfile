FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/integration-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 8070
ENTRYPOINT ["java","-jar","/app/app.jar"]

