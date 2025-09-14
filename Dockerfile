# === Этап сборки (builder) ===
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем pom.xml и прогреваем кеш зависимостей
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Копируем исходники и собираем jar
COPY src ./src
RUN mvn -q -B package -DskipTests

# === Этап рантайма (тонкий JRE) ===
FROM eclipse-temurin:17-jre
WORKDIR /app

# Копируем собранный jar с предыдущего этапа
COPY --from=build /app/target/task-manager-0.0.1-SNAPSHOT.jar app.jar

# Опции JVM и профиль Spring
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]