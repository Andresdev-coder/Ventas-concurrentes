# Primera etapa: compila el proyecto usando Maven y Java 17.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Descarga dependencias con anticipación para que la compilación posterior las reutilice.
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# Segunda etapa liviana: solo necesita Java para ejecutar el JAR ya compilado.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ventas-concurrentes-1.0.0.jar app.jar
# Puerto donde Spring Boot inicia el servidor HTTP.
EXPOSE 8080
# Inicia la aplicación cuando Docker arranca el contenedor.
ENTRYPOINT ["java", "-jar", "app.jar"]
