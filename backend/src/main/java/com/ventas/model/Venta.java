package com.ventas.model;

/**
 * Datos de una venta que se guardan en PostgreSQL y se devuelven como JSON.
 * Los datos descriptivos son inmutables; el cajero actualiza estado, nombre y mensaje.
 */
public class Venta {
    private final long id;
    private final String cliente;
    private final int productoId;
    private final String producto;
    private final int cantidad;
    private final long creadaEnMs;

    // volatile hace visibles inmediatamente los cambios del hilo cajero a otros hilos Java.
    // La BD también guarda estos campos para mantener el historial entre reinicios.
    private volatile EstadoVenta estado = EstadoVenta.PENDIENTE;
    private volatile String cajero;
    private volatile String mensaje;

    /** Construye una venta nueva y asigna la hora actual en milisegundos. */
    public Venta(long id, String cliente, Producto producto, int cantidad) {
        this(id, cliente, producto, cantidad, System.currentTimeMillis());
    }

    /** Constructor completo: permite reconstruir una venta con su fecha original desde SQL. */
    public Venta(long id, String cliente, Producto producto, int cantidad, long creadaEnMs) {
        this.id = id;
        this.cliente = cliente;
        this.productoId = producto.getId();
        this.producto = producto.getNombre();
        this.cantidad = cantidad;
        this.creadaEnMs = creadaEnMs;
    }

    // Getters permiten a Jackson convertir el objeto en la respuesta JSON de Spring.
    public long getId() { return id; }
    public String getCliente() { return cliente; }
    public int getProductoId() { return productoId; }
    public String getProducto() { return producto; }
    public int getCantidad() { return cantidad; }
    public long getCreadaEnMs() { return creadaEnMs; }
    public EstadoVenta getEstado() { return estado; }
    public String getCajero() { return cajero; }
    public String getMensaje() { return mensaje; }

    // El hilo cajero usa estos setters al avanzar la venta por sus estados.
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public void setCajero(String cajero) { this.cajero = cajero; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
