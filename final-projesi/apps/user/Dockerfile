FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY packages/lib/pom.xml packages/lib/pom.xml
COPY packages/lib/src packages/lib/src
RUN mvn -f packages/lib/pom.xml install -DskipTests -q

COPY apps/user/pom.xml apps/user/pom.xml
RUN mvn -f apps/user/pom.xml dependency:resolve -q 2>/dev/null || true

COPY apps/user/src apps/user/src
RUN mvn -f apps/user/pom.xml clean package -DskipTests -q


FROM gcr.io/distroless/java21-debian12
WORKDIR /app
COPY --from=build /app/apps/user/target/*.jar app.jar
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
