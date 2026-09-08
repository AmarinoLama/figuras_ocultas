# STAGE 1 : BUILD THE APPLICATION

FROM maven:3.9-eclipse-temurin-17 AS build

# Establecemos el directorio de trabajo
WORKDIR /app

# Configuración de Maven: redirige repos HTTP a Maven Central
# (evita el error "Blocked mirror for repositories" en dependency:go-offline)
COPY docker/settings.xml /root/.m2/settings.xml

# Copiamos SOLO el wrapper de Maven y el pom.xml primero.
# Así Docker puede cachear la capa de descarga de dependencias:
# si pom.xml no cambia, Maven no vuelve a descargar las librerías.
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY pom.xml pom.xml

# Descargamos todas las dependencias y las dejamos en la caché de Docker
# (-ntp elimina el ruido de "Progress (N): X/Y kB" de las descargas)
RUN chmod +x mvnw
RUN ./mvnw -ntp dependency:go-offline

# Copiamos el código fuente y compilamos
# (aprovecha la capa anterior: solo recompila, no redescarga dependencias)
COPY src src
RUN ./mvnw -ntp package -DskipTests

# STAGE 2 : RUN THE APPLICATION
FROM eclipse-temurin:17-jre-jammy

# Establecemos el directorio de trabajo
WORKDIR /app

# Copiamos el jar construido (usa wildcard para cualquier nombre)
COPY --from=build /app/target/*.jar app.jar

# Exponemos puerto informativo (Spring usará PORT env si lo configuras)
EXPOSE 8080

# Comando para ejecutar la app
ENTRYPOINT ["java","-jar","app.jar"]