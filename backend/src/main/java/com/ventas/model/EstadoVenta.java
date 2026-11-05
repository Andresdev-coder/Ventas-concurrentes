package com.ventas.model;

public enum EstadoVenta {
    PENDIENTE,    // en la cola, esperando un cajero libre
    EN_PROCESO,   // un hilo cajero la esta atendiendo
    COMPLETADA,   // stock descontado
    RECHAZADA     // no habia stock suficiente
}
