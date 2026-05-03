FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY apps/config/pom.xml apps/config/pom.xml
RUN mvn -f apps/config/pom.xml dependency:resolve -q 2>/dev/null || true

COPY apps/config/src apps/config/src
RUN mvn -f apps/config/pom.xml clean package -DskipTests -q


FROM gcr.io/distroless/java21-debian12
WORKDIR /app
COPY --from=build /app/apps/config/target/*.jar app.jar
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
