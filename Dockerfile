# syntax=docker/dockerfile:1

# ---------- Build ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src

# Primero solo el pom para cachear dependencias entre builds
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
# Los tests corren en CI (necesitan Docker para Testcontainers); acá solo se empaqueta
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /extracted

# ---------- Runtime ----------
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# Parches del SO publicados después de la imagen base (si no, Trivy los marca)
RUN apt-get update \
 && apt-get upgrade -y --no-install-recommends \
 && rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 1001 app \
 && useradd --system --uid 1001 --gid app --no-create-home app

# Capas de menos a más cambiantes: un cambio de código solo invalida la última
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./

USER 1001:1001
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Dpdfbox.fontcache=/tmp"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
