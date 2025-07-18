DROP DATABASE IF EXISTS figuras_ocultas;
CREATE DATABASE IF NOT EXISTS figuras_ocultas;
USE figuras_ocultas;

/* ========================= CREACIÓN DE LAS TABLAS  ========================= */

DROP TABLE IF EXISTS tarjeta_alumno;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS cartas;

CREATE TABLE IF NOT EXISTS usuarios
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre   CHAR(25) NOT NULL,
    email    VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    curso    ENUM ('PRIMERO_ESO', 'SEGUNDO_ESO', 'TERCERO_ESO', 'CUARTO_ESO', 'PRIMERO_BACH', 'SEGUNDO_BACH') NULL,
    rol      ENUM ('ADMIN', 'ALUMNO') NOT NULL
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

/* =========================  CREACIÓN DE TRIGGERS  ========================= */

DELIMITER $$
CREATE TRIGGER actualizar_nivel_tarjeta
BEFORE INSERT ON tarjeta_alumno
FOR EACH ROW
BEGIN
    IF NEW.exp >= 100 AND NEW.exp < 250 THEN
        SET NEW.nivel = 1;
    ELSEIF NEW.exp >= 250 AND NEW.exp < 500 THEN
        SET NEW.nivel = 2;
    ELSEIF NEW.exp >= 500 AND NEW.exp < 900 THEN
        SET NEW.nivel = 3;
    ELSEIF NEW.exp >= 900 AND NEW.exp < 1200 THEN
        SET NEW.nivel = 4;
    ELSEIF NEW.exp >= 1200 AND NEW.exp < 1700 THEN
        SET NEW.nivel = 5;
    ELSEIF NEW.exp >= 1700 THEN
        SET NEW.nivel = 6;
    ELSE
        SET NEW.nivel = 0;
    END IF;
END$$

CREATE TRIGGER actualizar_nivel_tarjeta_update
BEFORE UPDATE ON tarjeta_alumno
FOR EACH ROW
BEGIN
    IF NEW.exp >= 100 AND NEW.exp < 250 THEN
        SET NEW.nivel = 1;
    ELSEIF NEW.exp >= 250 AND NEW.exp < 500 THEN
        SET NEW.nivel = 2;
    ELSEIF NEW.exp >= 500 AND NEW.exp < 900 THEN
        SET NEW.nivel = 3;
    ELSEIF NEW.exp >= 900 AND NEW.exp < 1200 THEN
        SET NEW.nivel = 4;
    ELSEIF NEW.exp >= 1200 AND NEW.exp < 1700 THEN
        SET NEW.nivel = 5;
    ELSEIF NEW.exp >= 1700 THEN
        SET NEW.nivel = 6;
    ELSE
        SET NEW.nivel = 0;
    END IF;
END$$

DELIMITER ;

INSERT INTO usuarios (nombre, email, password, curso, rol)
VALUES 
    ('adminPrueba', 'admin@ua', '123', NULL, 'ADMIN'),
    ('alumno', 'alumno@ua', '123', 'PRIMERO_ESO', 'ALUMNO'),
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

INSERT INTO tarjeta_alumno (exp, electronios, usuario_id) VALUES
(0, 5, 2),
(100, 7, 3),
(50, 6, 4),
(230, 10, 5),
(70, 6, 6),
(0, 5, 7),
(10, 5, 8),
(5, 5, 9),
(150, 9, 10),
(200, 10, 11),
(180, 9, 12),
(250, 10, 13),
(300, 12, 14),
(320, 13, 15),
(280, 11, 16),
(310, 12, 17),
(295, 12, 18);

INSERT INTO cartas (imagen, precio, titulo, descripcion, activa) VALUES
                                                                     (NULL, 1, 'Carta de Curación', 'Restaura 50 puntos de vida al instante.', TRUE),
                                                                     (NULL, 1, 'Carta de Invisibilidad', 'Otorga invisibilidad durante 5 segundos.', TRUE),
                                                                     (NULL, 2, 'Carta de Rayo', 'Lanza un rayo que inflige daño en área.', FALSE),
                                                                     (NULL, 2, 'Carta de Congelación', 'Congela al enemigo durante 3 segundos.', FALSE),
                                                                     (NULL, 3, 'Carta de Velocidad', 'Aumenta la velocidad de movimiento un 30%.', TRUE),
                                                                     (NULL, 3, 'Carta de Veneno', 'Aplica veneno que causa daño durante 10 segundos.', TRUE),
                                                                     (NULL, 3, 'Carta de Escudo Mágico', 'Bloquea el siguiente ataque recibido.', TRUE),
                                                                     (NULL, 1, 'Carta de Ilusión', 'Crea un clon que distrae al enemigo.', TRUE);
