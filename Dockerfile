# ==================== Stage 1: Build Frontend ====================
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend
COPY junjie-rag-front/package.json junjie-rag-front/package-lock.json ./
RUN npm ci
COPY junjie-rag-front/ .
RUN npm run build

# ==================== Stage 2: Build Backend ====================
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

RUN mvn clean package -DskipTests -q

# ==================== Stage 3: Runtime ====================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=backend-builder /app/target/*.jar app.jar

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8989

ENV TZ=Asia/Shanghai

ENTRYPOINT ["java", "-jar", "app.jar"]
