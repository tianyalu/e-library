# ---------- stage 1: build (Maven + JDK 17) ----------
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

# ---------- stage 2: runtime (JRE only, non-root) ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S applib && adduser -S applib -G applib
COPY --from=builder /build/target/*.jar app.jar

USER applib
EXPOSE 8080 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
