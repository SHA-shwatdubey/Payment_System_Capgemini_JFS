FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/notification-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 8069
ENTRYPOINT ["java","-jar","/app/app.jar"]

