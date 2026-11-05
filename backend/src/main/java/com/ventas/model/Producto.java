package com.ventas.model;

/** Producto consultado desde PostgreSQL. */
public class Producto {
    private final int id;
    private final String nombre;
    private final int stockInicial;
    private final int stock;

    public Producto(int id, String nombre, int stockInicial) {
        this(id, nombre, stockInicial, stockInicial);
    }

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
