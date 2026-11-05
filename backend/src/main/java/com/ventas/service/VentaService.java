package com.ventas.service;

import com.ventas.model.EstadoVenta;
import com.ventas.model.Producto;
import com.ventas.model.Venta;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VentaService {
    private final InventarioService inventario;
    private final JdbcTemplate jdbc;
    private final ThreadPoolExecutor cajeros;
    private final int numCajeros;

    public VentaService(InventarioService inventario, JdbcTemplate jdbc,
                        @Value("${app.cajeros:4}") int numCajeros) {
        this.inventario = inventario;
        this.jdbc = jdbc;
        this.numCajeros = numCajeros;
        AtomicInteger contadorHilos = new AtomicInteger(1);
        this.cajeros = new ThreadPoolExecutor(numCajeros, numCajeros,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                r -> new Thread(r, "Cajero-" + contadorHilos.getAndIncrement()));
    }

    public Venta registrar(String cliente, int productoId, int cantidad) {
        Producto producto = inventario.buscar(productoId);
        Long id = jdbc.queryForObject("""
                INSERT INTO ventas (cliente, producto_id, producto, cantidad, creada_en_ms, estado)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, cliente, productoId, producto.getNombre(), cantidad,
                System.currentTimeMillis(), EstadoVenta.PENDIENTE.name());
        Venta venta = new Venta(id, cliente, producto, cantidad);
        cajeros.execute(() -> procesar(venta));
        return venta;
    }

    private void procesar(Venta venta) {
        venta.setCajero(Thread.currentThread().getName());
        venta.setEstado(EstadoVenta.EN_PROCESO);
        guardarEstado(venta);
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(800, 3000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            venta.setMensaje("Venta interrumpida");
            venta.setEstado(EstadoVenta.RECHAZADA);
            guardarEstado(venta);
            return;
        }

        if (inventario.descontar(venta.getProductoId(), venta.getCantidad())) {
            venta.setMensaje("Venta exitosa");
            venta.setEstado(EstadoVenta.COMPLETADA);
        } else {
            venta.setMensaje("Stock insuficiente");
            venta.setEstado(EstadoVenta.RECHAZADA);
        }
        guardarEstado(venta);
    }

    private void guardarEstado(Venta venta) {
        jdbc.update("UPDATE ventas SET estado = ?, cajero = ?, mensaje = ? WHERE id = ?",
                venta.getEstado().name(), venta.getCajero(), venta.getMensaje(), venta.getId());
    }

    public int simular(int cantidad) {
        String[] nombres = {"Ana", "Luis", "Maria", "Carlos", "Sofia", "Pedro", "Laura",
                "Andres", "Camila", "Jorge", "Valeria", "Daniel", "Paula", "Felipe"};
        List<Producto> productos = inventario.listar();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < cantidad; i++) {
            Producto p = productos.get(rnd.nextInt(productos.size()));
            registrar(nombres[rnd.nextInt(nombres.length)], p.getId(), 1 + rnd.nextInt(3));
        }
        return cantidad;
    }

    public List<Venta> listar() {
        return jdbc.query("""
                SELECT id, cliente, producto_id, producto, cantidad, creada_en_ms, estado, cajero, mensaje
                FROM ventas ORDER BY id DESC LIMIT 50
                """, (rs, row) -> {
            Venta venta = new Venta(rs.getLong("id"), rs.getString("cliente"),
                    new Producto(rs.getInt("producto_id"), rs.getString("producto"), 0),
                    rs.getInt("cantidad"), rs.getLong("creada_en_ms"));
            venta.setEstado(EstadoVenta.valueOf(rs.getString("estado")));
            venta.setCajero(rs.getString("cajero"));
            venta.setMensaje(rs.getString("mensaje"));
            return venta;
        });
    }

    public Resumen resumen() {
        int pendientes = contar(EstadoVenta.PENDIENTE);
        int enProceso = contar(EstadoVenta.EN_PROCESO);
        int completadas = contar(EstadoVenta.COMPLETADA);
        int rechazadas = contar(EstadoVenta.RECHAZADA);
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM ventas", Integer.class);
        int vendidas = jdbc.queryForObject("SELECT COALESCE(SUM(cantidad), 0) FROM ventas WHERE estado = ?",
                Integer.class, EstadoVenta.COMPLETADA.name());
        Boolean consistente = null;
        if (pendientes == 0 && enProceso == 0) {
            consistente = inventario.unidadesDescontadas() == vendidas;
        }
        return new Resumen(total, pendientes, enProceso, completadas, rechazadas,
                vendidas, numCajeros, cajeros.getActiveCount(), cajeros.getQueue().size(), consistente);
    }

    private int contar(EstadoVenta estado) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ventas WHERE estado = ?", Integer.class, estado.name());
    }

    public boolean reiniciar() {
        Resumen r = resumen();
        if (r.pendientes() > 0 || r.enProceso() > 0) return false;
        jdbc.update("DELETE FROM ventas");
        jdbc.execute("ALTER SEQUENCE ventas_id_seq RESTART WITH 1");
        inventario.reiniciar();
        return true;
    }

    @PreDestroy
    void cerrar() { cajeros.shutdownNow(); }
}
