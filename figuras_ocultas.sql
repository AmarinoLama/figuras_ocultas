DROP DATABASE IF EXISTS figuras_ocultas;
CREATE DATABASE IF NOT EXISTS figuras_ocultas;
USE figuras_ocultas;

DROP TABLE IF EXISTS usuarios;
CREATE TABLE IF NOT EXISTS usuarios
(
    id       INT AUTO_INCREMENT PRIMARY KEY,
    nombre   CHAR(25)                                                                                         NOT NULL,
    email    VARCHAR(100)                                                                                     NOT NULL UNIQUE,
    password VARCHAR(255)                                                                                     NOT NULL,
    curso    ENUM ('PRIMERO_ESO', 'SEGUNDO_ESO', 'TERCERO_ESO', 'CUARTO_ESO', 'PRIMERO_BACH', 'SEGUNDO_BACH') NULL,
    rol      ENUM ('ADMIN', 'ALUMNO')                                                                         NOT NULL
);

INSERT INTO usuarios (nombre, email, password, curso, rol)
VALUES ('adminPrueba', 'admin', '123', NULL, 'ADMIN'),
       ('alumno', 'alumno', '123', 'PRIMERO_ESO', 'ALUMNO'),
       ('Vegete Pérez', 'Vegete', '123', 'SEGUNDO_ESO', 'ALUMNO'),
       ('Skibidi Pérez', 'Skibidi', '123', 'SEGUNDO_ESO', 'ALUMNO'),
       ('Vicente Pérez', 'Vicente', '123', 'PRIMERO_BACH', 'ALUMNO'),
       ('Aman Pérez', 'Aman', '123', 'TERCERO_ESO', 'ALUMNO'),
       ('Izan S.L. Pérez', 'Izan', '123', 'PRIMERO_ESO', 'ALUMNO'),
       ('Carlos Martínez', 'Carlos', '123', 'PRIMERO_ESO', 'ALUMNO'),
       ('Ana López', 'Ana', '123', 'PRIMERO_ESO', 'ALUMNO'),
       ('Marta García', 'Marta', '123', 'SEGUNDO_ESO', 'ALUMNO'),
       ('Luis González', 'Luis', '123', 'SEGUNDO_ESO', 'ALUMNO'),
       ('José Sánchez', 'José', '123', 'TERCERO_ESO', 'ALUMNO'),
       ('Paula Rodríguez', 'Paula', '123', 'TERCERO_ESO', 'ALUMNO'),
       ('Raúl Pérez', 'Raúl', '123', 'CUARTO_ESO', 'ALUMNO'),
       ('Lucía Fernández', 'Lucía', '123', 'CUARTO_ESO', 'ALUMNO'),
       ('Santiago Díaz', 'Santiago', '123', 'PRIMERO_BACH', 'ALUMNO'),
       ('Clara Jiménez', 'Clara', '123', 'SEGUNDO_BACH', 'ALUMNO'),
       ('Martín Castro', 'Martín', '123', 'SEGUNDO_BACH', 'ALUMNO');