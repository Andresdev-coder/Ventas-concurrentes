package com.ventas.model;

/**
 * Representación de un producto leído de PostgreSQL y expuesto por la API.
 * Cada objeto contiene una instantánea del stock leída en una consulta.
 */
public class Producto {
    private final int id;
    private final String nombre;
    private final int stockInicial;
    private final int stock;

    /** Atajo útil para representar un producto cuyo stock inicial y actual coinciden. */
    public Producto(int id, String nombre, int stockInicial) {
        this(id, nombre, stockInicial, stockInicial);
    }

    /** Crea el producto con stock inicial y stock actual explícitos. */
    public Producto(int id, String nombre, int stockInicial, int stock) {
        this.id = id;
        this.nombre = nombre;
        this.stockInicial = stockInicial;
        this.stock = stock;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getStockInicial() { return stockInicial; }
    public int getStock() { return stock; }
}
    // Estos getters son usados por Jackson para formar el JSON enviado al frontend.
