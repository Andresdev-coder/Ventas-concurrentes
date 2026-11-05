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

/**
 * Coordina el ciclo de vida de las ventas y el trabajo concurrente de los cajeros.
 * La base de datos conserva el estado; el pool solo decide qué hilo procesa cada tarea.
 */
@Service
public class VentaService {
    /** Acceso al inventario persistido y a su descuento atómico. */
    private final InventarioService inventario;
    /** JdbcTemplate puede compartirse entre hilos: obtiene conexiones del pool de Spring. */
    private final JdbcTemplate jdbc;
    /** Pool fijo que ejecuta en paralelo las ventas aceptadas por la API. */
    private final ThreadPoolExecutor cajeros;
    /** Configuración usada para mostrar cuántos cajeros hay disponibles. */
    private final int numCajeros;

    /**
     * Crea exactamente N trabajadores y una cola para las tareas que no caben todavía.
     * Si llegan más ventas que cajeros, las ventas esperan en la cola, no crean hilos nuevos.
     */
    public VentaService(InventarioService inventario, JdbcTemplate jdbc,
                        @Value("${app.cajeros:4}") int numCajeros) {
        this.inventario = inventario;
        this.jdbc = jdbc;
        this.numCajeros = numCajeros;
        // AtomicInteger es necesario porque varios hilos pueden pedir nombre al mismo tiempo.
        AtomicInteger contadorHilos = new AtomicInteger(1);
        // corePoolSize == maximumPoolSize mantiene el tamaño fijo. La cola es segura entre hilos.
        this.cajeros = new ThreadPoolExecutor(numCajeros, numCajeros,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                r -> new Thread(r, "Cajero-" + contadorHilos.getAndIncrement()));
    }

    /** Guarda primero la venta como PENDIENTE y luego la envía a la cola de cajeros. */
    public Venta registrar(String cliente, int productoId, int cantidad) {
        Producto producto = inventario.buscar(productoId);
        // PostgreSQL genera el ID. Los signos ? evitan concatenar datos del usuario en el SQL.
        Long id = jdbc.queryForObject("""
                INSERT INTO ventas (cliente, producto_id, producto, cantidad, creada_en_ms, estado)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, cliente, productoId, producto.getNombre(), cantidad,
                System.currentTimeMillis(), EstadoVenta.PENDIENTE.name());
        Venta venta = new Venta(id, cliente, producto, cantidad);
        // La lambda encapsula el trabajo y execute lo asigna a un cajero disponible.
        cajeros.execute(() -> procesar(venta));
        return venta;
    }

    /** Trabajo que corre en un hilo Cajero-N y no en el hilo que atendió la petición HTTP. */
    private void procesar(Venta venta) {
        venta.setCajero(Thread.currentThread().getName());
        venta.setEstado(EstadoVenta.EN_PROCESO);
        guardarEstado(venta);
        try {
            // Simulación de tiempo de atención para hacer visible el trabajo concurrente.
            Thread.sleep(ThreadLocalRandom.current().nextLong(800, 3000));
        } catch (InterruptedException e) {
            // Se restaura la señal de interrupción y se deja registrada la venta como rechazada.
            Thread.currentThread().interrupt();
            venta.setMensaje("Venta interrumpida");
            venta.setEstado(EstadoVenta.RECHAZADA);
            guardarEstado(venta);
            return;
        }

        // No se usa un lock local: el UPDATE condicional en PostgreSQL protege también
        // ante varios hilos o incluso varias instancias del backend.
        if (inventario.descontar(venta.getProductoId(), venta.getCantidad())) {
            venta.setMensaje("Venta exitosa");
            venta.setEstado(EstadoVenta.COMPLETADA);
        } else {
            venta.setMensaje("Stock insuficiente");
            venta.setEstado(EstadoVenta.RECHAZADA);
        }
        guardarEstado(venta);
    }

    /** Persiste los cambios de estado para que consultas posteriores los lean desde la BD. */
    private void guardarEstado(Venta venta) {
        jdbc.update("UPDATE ventas SET estado = ?, cajero = ?, mensaje = ? WHERE id = ?",
                venta.getEstado().name(), venta.getCajero(), venta.getMensaje(), venta.getId());
    }

    /** Encola varias solicitudes rápidamente para observar trabajadores y espera en cola. */
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

    /** Devuelve las 50 ventas más recientes y reconstruye los objetos desde las filas SQL. */
    public List<Venta> listar() {
        return jdbc.query("""
                SELECT id, cliente, producto_id, producto, cantidad, creada_en_ms, estado, cajero, mensaje
                FROM ventas ORDER BY id DESC LIMIT 50
        """, (rs, row) -> {
            // RowMapper convierte cada fila del ResultSet en el modelo que serializa la API.
            Venta venta = new Venta(rs.getLong("id"), rs.getString("cliente"),
                    new Producto(rs.getInt("producto_id"), rs.getString("producto"), 0),
                    rs.getInt("cantidad"), rs.getLong("creada_en_ms"));
            venta.setEstado(EstadoVenta.valueOf(rs.getString("estado")));
            venta.setCajero(rs.getString("cajero"));
            venta.setMensaje(rs.getString("mensaje"));
            return venta;
        });
    }

    /** Calcula contadores persistidos y métricas momentáneas del pool de hilos. */
    public Resumen resumen() {
        int pendientes = contar(EstadoVenta.PENDIENTE);
        int enProceso = contar(EstadoVenta.EN_PROCESO);
        int completadas = contar(EstadoVenta.COMPLETADA);
        int rechazadas = contar(EstadoVenta.RECHAZADA);
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM ventas", Integer.class);
        int vendidas = jdbc.queryForObject("SELECT COALESCE(SUM(cantidad), 0) FROM ventas WHERE estado = ?",
                Integer.class, EstadoVenta.COMPLETADA.name());
        // Mientras haya tareas en curso, el stock y la venta aún podrían estar actualizándose.
        Boolean consistente = null;
        if (pendientes == 0 && enProceso == 0) {
            consistente = inventario.unidadesDescontadas() == vendidas;
        }
        return new Resumen(total, pendientes, enProceso, completadas, rechazadas,
                vendidas, numCajeros, cajeros.getActiveCount(), cajeros.getQueue().size(), consistente);
    }

    /** Cuenta ventas de un estado usando SQL parametrizado. */
    private int contar(EstadoVenta estado) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ventas WHERE estado = ?", Integer.class, estado.name());
    }

    /** Reinicia ventas e inventario solo cuando ningún cajero está trabajando o esperando. */
    public boolean reiniciar() {
        Resumen r = resumen();
        if (r.pendientes() > 0 || r.enProceso() > 0) return false;
        jdbc.update("DELETE FROM ventas");
        jdbc.execute("ALTER SEQUENCE ventas_id_seq RESTART WITH 1");
        inventario.reiniciar();
        return true;
    }

    /** Spring llama este método al cerrar la aplicación para interrumpir tareas del pool. */
    @PreDestroy
    void cerrar() { cajeros.shutdownNow(); }
}
