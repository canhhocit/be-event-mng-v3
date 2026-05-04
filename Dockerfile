# Stage 1: build
FROM maven:3.9.12-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: create image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Command to run the application using sh to expand environment variables
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]