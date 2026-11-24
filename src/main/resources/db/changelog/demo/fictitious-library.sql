-- Base exclusiva de demostración. Los importes y todas las identidades son ficticios.
INSERT INTO categoria (id, nombre, descripcion, activa) VALUES
    (1, 'Cartografía arcana', 'Mapas y crónicas de exploración.', TRUE),
    (2, 'Botánica lunar', 'Guías sobre plantas y jardines fantásticos.', TRUE),
    (3, 'Historia mágica', 'Archivos y relatos de comunidades fantásticas.', TRUE);

INSERT INTO autor (id, nombre_completo, activo) VALUES
    (1, 'Vesper del Atlas', TRUE),
    (2, 'Silva Lunaflor', TRUE),
    (3, 'Arden Tinteruna', TRUE);

INSERT INTO libro (id, titulo, isbn, editorial, anio_publicacion, descripcion, categoria_id, activo) VALUES
    (1, 'Atlas de corredores cambiantes', '0000000000000', 'Ediciones Farol Azul', 2021,
     'Guía ilustrada para orientarse en pasillos que cambian de lugar al anochecer.', 1, TRUE),
    (2, 'Herbario del invernadero lunar', '0000000000001', 'Ediciones Semilla de Plata', 2019,
     'Cuaderno de campo con especies que germinan bajo distintas fases de la luna.', 2, TRUE),
    (3, 'Crónicas de la Torre de Tinta', '0000000000002', 'Ediciones Farol Azul', 2023,
     'Relatos de los primeros custodios de una biblioteca ambulante.', 3, TRUE);

INSERT INTO libro_autor (libro_id, autor_id) VALUES (1, 1), (2, 2), (3, 3), (3, 1);

INSERT INTO ejemplar (id, codigo, estado, fecha_alta, observaciones, precio_dia, libro_id) VALUES
    (1, 'HOG-000001', 'ALQUILADO', CURRENT_DATE - INTERVAL 60 DAY, NULL, 120.00, 1),
    (2, 'HOG-000002', 'DISPONIBLE', CURRENT_DATE - INTERVAL 60 DAY, 'Edición de consulta general.', 120.00, 1),
    (3, 'HOG-000003', 'DISPONIBLE', CURRENT_DATE - INTERVAL 45 DAY, NULL, 90.00, 2),
    (4, 'HOG-000004', 'DISPONIBLE', CURRENT_DATE - INTERVAL 45 DAY, NULL, 150.00, 3),
    (5, 'HOG-000005', 'FUERA_SERVICIO', CURRENT_DATE - INTERVAL 30 DAY, 'Encuadernación en reparación.', 150.00, 3),
    (6, 'HOG-000006', 'ALQUILADO', CURRENT_DATE - INTERVAL 30 DAY, NULL, 90.00, 2);

UPDATE secuencia_codigo SET ultimo_valor = 6 WHERE nombre = 'EJEMPLAR';

INSERT INTO cliente (id, nombre, apellido, dni, email, fecha_registro, activo) VALUES
    (1, 'Bruma', 'Farol', 'DEMO-001', 'bruma@example.invalid', CURRENT_DATE - INTERVAL 40 DAY, TRUE),
    (2, 'Junco', 'Papel', 'DEMO-002', 'junco@example.invalid', CURRENT_DATE - INTERVAL 35 DAY, TRUE),
    (3, 'Nimbo', 'Pergamino', 'DEMO-003', 'nimbo@example.invalid', CURRENT_DATE - INTERVAL 30 DAY, TRUE);

INSERT INTO alquiler (id, estado, fecha_alquiler, fecha_vencimiento, fecha_devolucion, observaciones, total, cliente_id) VALUES
    (1, 'ACTIVO', CURRENT_DATE - INTERVAL 2 DAY, CURRENT_DATE + INTERVAL 3 DAY, NULL,
     'Reserva de material para el taller de cartografía.', 600.00, 1),
    (2, 'FINALIZADO', CURRENT_DATE - INTERVAL 10 DAY, CURRENT_DATE - INTERVAL 6 DAY, CURRENT_DATE - INTERVAL 7 DAY,
     'Devolución anticipada.', 360.00, 2),
    (3, 'CANCELADO', CURRENT_DATE - INTERVAL 8 DAY, CURRENT_DATE - INTERVAL 5 DAY, NULL,
     'Solicitud cancelada antes del retiro.', 450.00, 3),
    (4, 'ACTIVO', CURRENT_DATE - INTERVAL 12 DAY, CURRENT_DATE - INTERVAL 7 DAY, NULL,
     'Ejemplo de un alquiler pendiente de devolución.', 450.00, 1);

INSERT INTO detalle_alquiler (id, cantidad_dias, precio_dia, subtotal, alquiler_id, ejemplar_id) VALUES
    (1, 5, 120.00, 600.00, 1, 1),
    (2, 4, 90.00, 360.00, 2, 3),
    (3, 3, 150.00, 450.00, 3, 4),
    (4, 5, 90.00, 450.00, 4, 6);

INSERT INTO pago (id, fecha, metodo_pago, monto, observaciones, alquiler_id) VALUES
    (1, CURRENT_DATE - INTERVAL 2 DAY, 'EFECTIVO', 200.00, 'Anticipo ficticio.', 1),
    (2, CURRENT_DATE - INTERVAL 10 DAY, 'TRANSFERENCIA', 160.00, 'Anticipo ficticio.', 2),
    (3, CURRENT_DATE - INTERVAL 7 DAY, 'TARJETA', 200.00, 'Saldo ficticio al devolver.', 2);
