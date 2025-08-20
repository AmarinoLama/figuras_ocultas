DROP DATABASE IF EXISTS figuras_ocultas;
CREATE DATABASE IF NOT EXISTS figuras_ocultas;
USE figuras_ocultas;

/* ========================= CREACIÓN DE LAS TABLAS  ========================= */

DROP TABLE IF EXISTS tarjeta_alumno;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS cartas;
DROP TABLE IF EXISTS historial_transacciones;
DROP TABLE IF EXISTS cartas_usuario;
DROP TABLE IF EXISTS web_config;
DROP TABLE IF EXISTS insignias;

CREATE TABLE IF NOT EXISTS usuarios
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre   CHAR(25) NOT NULL,
    email    VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    curso    ENUM ('PRIMERO_ESO', 'SEGUNDO_ESO', 'TERCERO_ESO', 'CUARTO_ESO', 'PRIMERO_BACH', 'SEGUNDO_BACH') NULL,
    rol      ENUM ('ADMIN', 'ALUMNO') NOT NULL
);

CREATE TABLE IF NOT EXISTS web_config (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     nombre VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS insignias (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     nombre VARCHAR(50) NOT NULL,
     imagen MEDIUMBLOB,
     alumno_id BIGINT NOT NULL,
     FOREIGN KEY (alumno_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tarjeta_alumno 
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nivel        TINYINT NOT NULL DEFAULT 0,
    exp          INT NOT NULL DEFAULT 0,
    electronios  TINYINT NOT NULL DEFAULT 0,
    usuario_id   BIGINT UNIQUE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cartas
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    imagen       MEDIUMBLOB NULL,
    precio       INT NOT NULL DEFAULT 0,
    titulo       VARCHAR(50) NOT NULL,
    descripcion  TEXT,
    activa       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS cartas_usuario (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      carta_id BIGINT NOT NULL,
      alumno_id BIGINT NOT NULL,
      fecha_adquisicion DATETIME DEFAULT CURRENT_TIMESTAMP,
      usada BOOLEAN DEFAULT FALSE,
      fecha_usada DATETIME NULL,
      FOREIGN KEY (carta_id) REFERENCES cartas(id) ON DELETE CASCADE,
      FOREIGN KEY (alumno_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS historial_transacciones (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       alumno_id BIGINT NOT NULL,
       tipo ENUM('COMPRA', 'USO', 'GANAR_ELECTRONIOS', 'PERDER_ELECTRONIOS') NOT NULL,
       descripcion TEXT,
       electronios_en_momento INT NOT NULL,
       fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
       FOREIGN KEY (alumno_id) REFERENCES usuarios(id) ON DELETE CASCADE
);


/* =========================  CREACIÓN DE TRIGGERS  ========================= */

-- Trigger para INSERT
CREATE TRIGGER actualizar_nivel_tarjeta
    BEFORE INSERT ON tarjeta_alumno
    FOR EACH ROW
    SET NEW.nivel = CASE
                        WHEN NEW.exp >= 1700 THEN 6
                        WHEN NEW.exp >= 1200 THEN 5
                        WHEN NEW.exp >= 900  THEN 4
                        WHEN NEW.exp >= 500  THEN 3
                        WHEN NEW.exp >= 250  THEN 2
                        WHEN NEW.exp >= 100  THEN 1
                        ELSE 0
        END;

-- Trigger para UPDATE
CREATE TRIGGER actualizar_nivel_tarjeta_update
    BEFORE UPDATE ON tarjeta_alumno
    FOR EACH ROW
    SET NEW.nivel = CASE
                        WHEN NEW.exp >= 1700 THEN 6
                        WHEN NEW.exp >= 1200 THEN 5
                        WHEN NEW.exp >= 900  THEN 4
                        WHEN NEW.exp >= 500  THEN 3
                        WHEN NEW.exp >= 250  THEN 2
                        WHEN NEW.exp >= 100  THEN 1
                        ELSE 0
        END;