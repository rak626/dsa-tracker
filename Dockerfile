# ---------- Build Stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only pom.xml first (for layer caching)
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build jar (skip tests if you want)
RUN mvn clean package -DskipTests


# ---------- Runtime Stage ----------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy your Spring config
COPY src/main/resources/application-prod.yaml /app/application.yaml

EXPOSE 5050

ENTRYPOINT ["java","-jar","/app/app.jar","--spring.config.location=file:/app/application.yaml"]
