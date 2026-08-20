FROM node:20-alpine AS frontend-builder
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-builder
WORKDIR /workspace
COPY pom.xml ./
COPY .mvn .mvn
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
COPY sql ./sql
RUN mkdir -p ./src/main/resources/static
COPY --from=frontend-builder /workspace/src/main/resources/static/ ./src/main/resources/static/
RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-builder /workspace/target/college-recommendation-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
