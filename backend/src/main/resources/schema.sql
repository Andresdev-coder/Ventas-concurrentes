-- Inventario persistente: el CHECK impide guardar stocks negativos.
CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    stock_inicial INTEGER NOT NULL CHECK (stock_inicial >= 0),
    stock INTEGER NOT NULL CHECK (stock >= 0)
);

-- Historial de ventas. BIGSERIAL asigna IDs crecientes automáticamente.
CREATE TABLE IF NOT EXISTS ventas (
    id BIGSERIAL PRIMARY KEY,
    cliente VARCHAR(160) NOT NULL,
    producto_id INTEGER NOT NULL REFERENCES productos(id),
    producto VARCHAR(120) NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    creada_en_ms BIGINT NOT NULL,
    estado VARCHAR(30) NOT NULL,
    cajero VARCHAR(80),
    mensaje VARCHAR(240)
);

-- Acelera el ordenamiento de ventas recientes por ID.
CREATE INDEX IF NOT EXISTS idx_ventas_creada ON ventas (id DESC);
