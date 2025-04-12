DROP DATABASE IF EXISTS figuras_ocultas;
CREATE DATABASE IF NOT EXISTS figuras_ocultas;
USE figuras_ocultas;

DROP TABLE IF EXISTS usuarios;
CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre CHAR(25) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN', 'ALUMNO') NOT NULL
);

INSERT INTO usuarios (nombre, email, password, rol) VALUES
('Aman', 'admin', '123', 'ADMIN'),
('Juan Pérez', 'alumno', '123', 'ALUMNO');