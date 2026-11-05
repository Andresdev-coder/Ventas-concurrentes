package com.ventas.model;

/** Estados posibles del recorrido de una venta; los nombres también se guardan en PostgreSQL. */
public enum EstadoVenta {
    PENDIENTE,    // en la cola, esperando un cajero libre
    EN_PROCESO,   // un hilo cajero la esta atendiendo
    COMPLETADA,   // stock descontado
    RECHAZADA     // no habia stock suficiente
}
