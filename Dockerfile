# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copia tutto il progetto
COPY . .

# Rendi eseguibile gradlew
RUN chmod +x ./gradlew

# Build del fat jar
RUN ./gradlew fatJar --no-daemon


# ---- Runtime stage ----
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copia solo il jar (più leggero)
COPY --from=builder /app/build/libs/*all.jar app.jar

# Porta Railway (non obbligatorio ma utile)
EXPOSE 8080

# Avvio applicazione
CMD ["java", "-jar", "app.jar"]