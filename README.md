# Figuras Ocultas

Aplicacion web educativa desarrollada con **Spring Boot** y **Thymeleaf** para el curso de `AmarinoLama`. Permite a los usuarios (alumnos y administradores) acceder a una plataforma interactiva de aprendizaje sobre figuras geometricas ocultas.

---

## Funcionalidades

- **Sistema de login** con autenticacion basada en email y password
- **Roles de usuario**: `ADMIN` y `ALUMNO`, con panel de administracion para gestionar usuarios
- **CRUD completo** de usuarios (crear, leer, actualizar, eliminar)
- **Vista principal** accesible tras el login
- **Documentacion API** integrada via Springdoc OpenAPI (Swagger UI)

---

## Tecnologias

| Componente | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.4.1 |
| Plantillas | Thymeleaf |
| Base de datos | MySQL 8 / H2 (desarrollo) |
| ORM | Spring Data JPA + Hibernate |
| Mapeo DTO | ModelMapper 3.0 |
| Boilerplate | Lombok |
| API Docs | Springdoc OpenAPI 1.6.6 |
| Contenedor | Docker + Docker Compose |

---

## Estructura del proyecto

```
src/main/java/edu/badpals/FigurasOcultas/
├── AccessingDataJpaApplication.java   # Punto de entrada
├── config/
│   └── ModelMapperConfig.java         # Configuracion de ModelMapper
├── controller/
│   ├── IndexController.java           # Controlador principal
│   └── LoginController.java           # Controlador de login
├── model/
│   ├── dto/
│   │   └── UsuarioDTO.java            # DTO de transferencia de datos
│   ├── entity/
│   │   ├── Usuario.java               # Entidad JPA
│   │   └── RolUsuario.java            # Enum de roles (ADMIN, ALUMNO)
│   └── repository/
│       └── UsuarioRepository.java     # Repositorio JPA
└── service/
    └── UsuarioService.java            # Logica de negocio
```

---

## Requisitos previos

- Java 21 o superior
- Maven 3.6+
- MySQL 8.0 (para produccion) o H2 (para desarrollo)

---

## Instalacion y ejecutacion

### 1. Clonar el repositorio

```bash
git clone https://github.com/AmarinoLama/figuras_ocultas.git
cd figuras_ocultas
```

### 2. Configurar la base de datos

Ejecuta el script SQL incluido para crear la base de datos y los usuarios de prueba:

```bash
mysql -u root -p < figuras_ocultas.sql
```

### 3. Ejecutar la aplicacion

```bash
./mvnw spring-boot:run
```

La aplicacion estara disponible en: **http://localhost:8080**

### 4. (Opcional) Ejecutar con Docker

```bash
docker-compose up --build
```

---

## Credenciales de prueba

| Usuario | Email | Password | Rol |
|---|---|---|---|
| Aman | admin | 123 | ADMIN |
| Juan Perez | alumno | 123 | ALUMNO |

---

## API REST (Swagger)

Una vez ejecutada la aplicacion, la documentacion interactiva de la API esta disponible en:

**http://localhost:8080/swagger-ui.html**

---

## Configuracion

Las propiedades principales se encuentran en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/figuras_ocultas
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
```

---

## Licencia

Proyecto educativo - `AmarinoLama`
