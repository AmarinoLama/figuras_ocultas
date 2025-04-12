DROP DATABASE IF EXISTS figuras_ocultas;
CREATE DATABASE IF NOT EXISTS figuras_ocultas;
USE figuras_ocultas;

DROP TABLE IF EXISTS usuarios;
CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre CHAR(25) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    curso CHAR(25) NULL,
    rol ENUM('ADMIN', 'ALUMNO') NOT NULL
);

INSERT INTO usuarios (nombre, email, password, curso, rol) VALUES
('adminPrueba', 'admin', '123', '1 ESO', 'ADMIN'),
('alumno', 'alumno', '123', '1 ESO', 'ALUMNO'),
('Vegete Pérez', 'Vegete', '123', '2 ESO', 'ALUMNO'),
('Skibidi Pérez', 'Skibidi', '123', '2 ESO', 'ALUMNO'),
('Vicente Pérez', 'Vicente', '123', '1 BAC', 'ALUMNO'),
('Aman Pérez', 'Aman', '123', '3 ESO', 'ALUMNO'),
('Izan S.L. Pérez', 'Izan', '123', '1 ESO', 'ALUMNO');