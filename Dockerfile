# Этап сборки
FROM maven:3.9-amazoncorretto-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап запуска
FROM amazoncorretto:17
WORKDIR /app
COPY --from=builder /app/target/telegram-bot-1.0-SNAPSHOT.jar ./bot.jar
ENV BOT_TOKEN=${BOT_TOKEN}
ENV BOT_USERNAME=${BOT_USERNAME}
ENV DB_URL=${DB_URL}
ENV DB_USER=${DB_USER}
ENV DB_PASSWORD=${DB_PASSWORD}
ENTRYPOINT ["java", "-jar", "bot.jar"]