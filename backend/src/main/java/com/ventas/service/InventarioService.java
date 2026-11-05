package com.ventas.service;

import com.ventas.model.Producto;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Lee y modifica el inventario persistido en PostgreSQL. */
@Service
public class InventarioService {
    /** Abstracción de Spring JDBC para ejecutar SQL sin administrar conexiones a mano. */
    private final JdbcTemplate jdbc;

    public InventarioService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Busca todos los productos y mapea cada fila de la tabla a un objeto Producto. */
    public List<Producto> listar() {
        return jdbc.query("SELECT id, nombre, stock_inicial, stock FROM productos ORDER BY id",
                (rs, row) -> new Producto(rs.getInt("id"), rs.getString("nombre"),
                        rs.getInt("stock_inicial"), rs.getInt("stock")));
    }

    /** Busca un producto por ID o responde HTTP 404 si no existe. */
    public Producto buscar(int id) {
        return jdbc.query("SELECT id, nombre, stock_inicial, stock FROM productos WHERE id = ?",
                        (rs, row) -> new Producto(rs.getInt("id"), rs.getString("nombre"),
                                rs.getInt("stock_inicial"), rs.getInt("stock")), id)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Producto no existe: " + id));
    }

    /**
     * Un único UPDATE hace la comprobación y el descuento como una operación atómica.
     * PostgreSQL bloquea la fila durante la actualización: dos cajeros no pueden gastar
     * simultáneamente las mismas unidades. Retorna true si modificó una fila (venta posible).
     */
    public boolean descontar(int productoId, int cantidad) {
        return jdbc.update("UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?",
                cantidad, productoId, cantidad) == 1;
    }

    /** Suma las unidades retiradas del inventario desde que se cargó el stock inicial. */
    public int unidadesDescontadas() {
        return jdbc.queryForObject("SELECT COALESCE(SUM(stock_inicial - stock), 0) FROM productos",
                Integer.class);
    }

    /** Repone el stock original de todos los productos. */
    public void reiniciar() {
        jdbc.update("UPDATE productos SET stock = stock_inicial");
    }
}
