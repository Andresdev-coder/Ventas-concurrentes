package com.ventas.service;

/**
 * Respuesta agregada de GET /api/resumen.
 * Record genera constructor, getters y una estructura inmutable para serializar como JSON.
 */
public record Resumen(
        int total,           // cantidad de filas de venta guardadas
        int pendientes,      // tareas que esperan atención
        int enProceso,       // tareas que ejecuta actualmente un cajero
        int completadas,     // ventas que descontaron stock
        int rechazadas,      // ventas que no pudieron descontar stock
        int unidadesVendidas,// suma de cantidades de las ventas completadas
        int cajeros,         // tamaño configurado del pool
        int cajerosActivos,  // hilos del pool que están ejecutando una tarea
        int enCola,          // tareas esperando un cajero libre
        Boolean consistente // null mientras hay ventas en curso; true/false al terminar
) {}
