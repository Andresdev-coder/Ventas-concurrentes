-- Carga los productos iniciales solo si aún no existen; no repone stock al reiniciar el backend.
INSERT INTO productos (id, nombre, stock_inicial, stock) VALUES
    (1, 'Laptop', 5, 5),
    (2, 'Mouse', 10, 10),
    (3, 'Teclado', 8, 8),
    (4, 'Monitor', 4, 4)
ON CONFLICT (id) DO NOTHING;
