# ---------- Estágio 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copia só o pom primeiro para aproveitar o cache de dependências do Docker
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes (e o JaCoCo) rodam pelo serviço "tests" do docker-compose
RUN mvn -B -q clean package -DskipTests

# ---------- Estágio 2: runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuário sem privilégios
RUN useradd --system --create-home appuser
COPY --from=build /build/target/guesthouse-api-0.0.1-SNAPSHOT.jar app.jar
USER appuser

ENV TZ=America/Sao_Paulo
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
