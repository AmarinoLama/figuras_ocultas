# STAGE 1 : BUILD THE APPLICATION

RUN chmod +x mvnw

FROM maven:3.9-eclipse-temurin-17 AS build

# Establecemos el directorio de trabajo
WORKDIR /app

# Copiamos el wrapper de Maven y el pom.xml
COPY . .

# Buildeamos la app
RUN ./mvnw clean package -DskipTests

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