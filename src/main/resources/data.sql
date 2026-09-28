-- Los cursos por defecto (1º ESO A ... 4º ESO C, códigos 1ESOA ... 4ESOC) los crea
-- la aplicación al arrancar: ver edu.badpals.FigurasOcultas.service.CursoSeedService.
INSERT INTO usuarios (nombre, email, password, curso_id, rol)
VALUES
    ('adminPrueba', 'admin@ua', '123', NULL, 'ADMIN'),

    -- 1º ESO
    ('Laura Pérez', 'laura@ua', '123', (SELECT id FROM cursos WHERE codigo = '1ESOA'), 'ALUMNO'),
    ('Diego Gómez', 'diego@ua', '123', (SELECT id FROM cursos WHERE codigo = '1ESOB'), 'ALUMNO'),
    ('Nerea Castro', 'nerea@ua', '123', (SELECT id FROM cursos WHERE codigo = '1ESOC'), 'ALUMNO'),

    -- 2º ESO
    ('Hugo Fernández', 'hugo@ua', '123', (SELECT id FROM cursos WHERE codigo = '2ESOA'), 'ALUMNO'),
    ('Valeria López', 'valeria@ua', '123', (SELECT id FROM cursos WHERE codigo = '2ESOB'), 'ALUMNO'),
    ('Iván Morales', 'ivan@ua', '123', (SELECT id FROM cursos WHERE codigo = '2ESOC'), 'ALUMNO'),

    -- 3º ESO
    ('Sofía Martín', 'sofia@ua', '123', (SELECT id FROM cursos WHERE codigo = '3ESOA'), 'ALUMNO'),
    ('Álvaro Díaz', 'alvaro@ua', '123', (SELECT id FROM cursos WHERE codigo = '3ESOB'), 'ALUMNO'),
    ('Paula Ruiz', 'paula@ua', '123', (SELECT id FROM cursos WHERE codigo = '3ESOC'), 'ALUMNO'),

    -- 4º ESO
    ('Mario Rodríguez', 'mario@ua', '123', (SELECT id FROM cursos WHERE codigo = '4ESOA'), 'ALUMNO'),
    ('Carmen Sánchez', 'carmen@ua', '123', (SELECT id FROM cursos WHERE codigo = '4ESOB'), 'ALUMNO'),
    ('Adrián Torres', 'adrian@ua', '123', (SELECT id FROM cursos WHERE codigo = '4ESOC'), 'ALUMNO');


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

INSERT INTO web_config (nombre) VALUES ('Almas Sagradas');