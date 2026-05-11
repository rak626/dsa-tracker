# ---------- Build Stage ----------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom first to cache dependencies layer
COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copy source and build
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B


# ---------- Runtime Stage ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Security: Create and use a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Use wildcard for JAR to avoid breaking build on version changes in pom.xml
COPY --from=build /app/target/dsa-tracker-*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

# Local-friendly environment variables
# - JAVA_OPTS: Lower memory limits for local execution
# - SPRING_PROFILES_ACTIVE: Default to prod, but easily overridable
ENV JAVA_OPTS="-Xms128m -Xmx2512m -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 5050

# Use exec form through sh -c to allow environment variable expansion
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
