package com.ventas.service;

public record Resumen(
        int total,
        int pendientes,
        int enProceso,
        int completadas,
        int rechazadas,
        int unidadesVendidas,
        int cajeros,
        int cajerosActivos,
        int enCola,
        Boolean consistente   // null mientras todavia hay ventas en curso
) {}
