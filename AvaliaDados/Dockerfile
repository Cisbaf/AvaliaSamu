# Estágio de construção (build)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copiar arquivos do projeto
COPY pom.xml .
COPY src ./src

# Baixar dependências e construir o projeto
RUN mvn clean install -DskipTests

# Estágio de execução (runtime)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar o JAR gerado para a imagem final
COPY --from=build /app/target/AvaliaDados-*.jar app.jar

# Expor a porta da aplicação
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]