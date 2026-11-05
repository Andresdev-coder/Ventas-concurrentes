package com.ventas.service;

import com.ventas.model.Producto;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InventarioService {
    private final JdbcTemplate jdbc;

    public InventarioService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Producto> listar() {
        return jdbc.query("SELECT id, nombre, stock_inicial, stock FROM productos ORDER BY id",
                (rs, row) -> new Producto(rs.getInt("id"), rs.getString("nombre"),
                        rs.getInt("stock_inicial"), rs.getInt("stock")));
    }

    public Producto buscar(int id) {
        return jdbc.query("SELECT id, nombre, stock_inicial, stock FROM productos WHERE id = ?",
                        (rs, row) -> new Producto(rs.getInt("id"), rs.getString("nombre"),
                                rs.getInt("stock_inicial"), rs.getInt("stock")), id)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Producto no existe: " + id));
    }

    /** Un solo UPDATE condicional hace que validar y descontar sea atomico en PostgreSQL. */
    public boolean descontar(int productoId, int cantidad) {
        return jdbc.update("UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?",
                cantidad, productoId, cantidad) == 1;
    }

    public int unidadesDescontadas() {
        return jdbc.queryForObject("SELECT COALESCE(SUM(stock_inicial - stock), 0) FROM productos",
                Integer.class);
    }

    public void reiniciar() {
        jdbc.update("UPDATE productos SET stock = stock_inicial");
    }
}
