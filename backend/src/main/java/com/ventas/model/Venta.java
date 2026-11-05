package com.ventas.model;

public class Venta {
    private final long id;
    private final String cliente;
    private final int productoId;
    private final String producto;
    private final int cantidad;
    private final long creadaEnMs;

    // volatile: el hilo cajero las escribe y el hilo HTTP las lee
    private volatile EstadoVenta estado = EstadoVenta.PENDIENTE;
    private volatile String cajero;
    private volatile String mensaje;

    public Venta(long id, String cliente, Producto producto, int cantidad) {
        this(id, cliente, producto, cantidad, System.currentTimeMillis());
    }

    public Venta(long id, String cliente, Producto producto, int cantidad, long creadaEnMs) {
        this.id = id;
        this.cliente = cliente;
        this.productoId = producto.getId();
        this.producto = producto.getNombre();
        this.cantidad = cantidad;
        this.creadaEnMs = creadaEnMs;
    }

    public long getId() { return id; }
    public String getCliente() { return cliente; }
    public int getProductoId() { return productoId; }
    public String getProducto() { return producto; }
    public int getCantidad() { return cantidad; }
    public long getCreadaEnMs() { return creadaEnMs; }
    public EstadoVenta getEstado() { return estado; }
    public String getCajero() { return cajero; }
    public String getMensaje() { return mensaje; }

    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public void setCajero(String cajero) { this.cajero = cajero; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
