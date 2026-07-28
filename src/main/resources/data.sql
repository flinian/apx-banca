-- ==========================================================================
-- Datos semilla del "mainframe" simulado.
-- Spring Boot ejecuta este data.sql despues de schema.sql en cada arranque.
-- Asi siempre tenemos datos conocidos para probar el caso de uso.
-- ==========================================================================

INSERT INTO CLIENTE (id, nombre, documento) VALUES
    (1, 'Ana Torres',   '40123456'),
    (2, 'Luis Ramirez', '41987654');

INSERT INTO CUENTA (id, cliente_id, numero_cuenta, tipo, saldo, moneda, estado) VALUES
    (100, 1, '0011-2233-4455', 'AHORRO',    1500.75, 'PEN', 'ACTIVA'),
    (101, 1, '0011-2233-9999', 'CORRIENTE', 320.00,  'USD', 'ACTIVA'),
    (102, 1, '0011-2233-0000', 'AHORRO',    0.00,    'PEN', 'BLOQUEADA'),
    (200, 2, '0022-3344-5566', 'AHORRO',    8750.50, 'PEN', 'ACTIVA');

INSERT INTO MOVIMIENTO (cuenta_id, tipo, monto, descripcion, fecha) VALUES
    (100, 'ABONO', 1500.75, 'Deposito inicial',        CURRENT_TIMESTAMP - 10),
    (101, 'ABONO', 320.00,  'Deposito inicial',        CURRENT_TIMESTAMP - 10),
    (200, 'ABONO', 9000.00, 'Deposito inicial',        CURRENT_TIMESTAMP - 9),
    (200, 'CARGO', 249.50,  'Pago servicio de luz',    CURRENT_TIMESTAMP - 3);
