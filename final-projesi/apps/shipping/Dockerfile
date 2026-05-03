FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY packages/lib/pom.xml packages/lib/pom.xml
COPY packages/lib/src packages/lib/src
RUN mvn -f packages/lib/pom.xml install -DskipTests -q

COPY apps/shipping/pom.xml apps/shipping/pom.xml
RUN mvn -f apps/shipping/pom.xml dependency:resolve -q 2>/dev/null || true

COPY apps/shipping/src apps/shipping/src
RUN mvn -f apps/shipping/pom.xml clean package -DskipTests -q


FROM gcr.io/distroless/java21-debian12
WORKDIR /app
COPY --from=build /app/apps/shipping/target/*.jar app.jar
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
