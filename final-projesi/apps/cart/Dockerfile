FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY packages/lib/pom.xml packages/lib/pom.xml
COPY packages/lib/src packages/lib/src
RUN mvn -f packages/lib/pom.xml install -DskipTests -q

COPY apps/cart/pom.xml apps/cart/pom.xml
RUN mvn -f apps/cart/pom.xml dependency:resolve -q 2>/dev/null || true

COPY apps/cart/src apps/cart/src
RUN mvn -f apps/cart/pom.xml clean package -DskipTests -q


FROM gcr.io/distroless/java21-debian12
WORKDIR /app
COPY --from=build /app/apps/cart/target/*.jar app.jar
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
