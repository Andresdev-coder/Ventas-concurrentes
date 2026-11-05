package com.ventas.controller;

import com.ventas.model.Producto;
import com.ventas.model.Venta;
import com.ventas.service.InventarioService;
import com.ventas.service.Resumen;
import com.ventas.service.VentaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Capa HTTP: valida las solicitudes y delega las reglas de negocio a los servicios. */
@RestController
@RequestMapping("/api")
public class VentaController {

    /** Forma del JSON que recibe POST /api/ventas. */
    public record VentaRequest(String cliente, int productoId, int cantidad) {}

    private final VentaService ventaService;
    private final InventarioService inventarioService;

    /** Constructor por inyección: Spring proporciona los servicios registrados como beans. */
    public VentaController(VentaService ventaService, InventarioService inventarioService) {
        this.ventaService = ventaService;
        this.inventarioService = inventarioService;
    }

    /** GET /api/productos: entrega el inventario actual. */
    @GetMapping("/productos")
    public List<Producto> productos() {
        return inventarioService.listar();
    }

    /** GET /api/ventas: consulta las ventas más recientes. */
    @GetMapping("/ventas")
    public List<Venta> ventas() {
        return ventaService.listar();
    }

    /** GET /api/resumen: muestra estados, cola de tareas y consistencia del inventario. */
    @GetMapping("/resumen")
    public Resumen resumen() {
        return ventaService.resumen();
    }

    /** POST /api/ventas: valida datos básicos y devuelve 202 mientras el cajero trabaja. */
    @PostMapping("/ventas")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Venta registrar(@RequestBody VentaRequest req) {
        if (req.cliente() == null || req.cliente().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente es obligatorio");
        }
        if (req.cantidad() < 1 || req.cantidad() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe estar entre 1 y 100");
        }
        // Una respuesta 202 Accepted significa que la tarea quedó aceptada, no que ya terminó.
        return ventaService.registrar(req.cliente().trim(), req.productoId(), req.cantidad());
    }

    /** POST /api/ventas/simular?cantidad=N: acepta entre 1 y 100 ventas de prueba. */
    @PostMapping("/ventas/simular")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Integer> simular(@RequestParam(defaultValue = "20") int cantidad) {
        if (cantidad < 1 || cantidad > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad entre 1 y 100");
        }
        return Map.of("generadas", ventaService.simular(cantidad));
    }

    /** POST /api/reiniciar: devuelve 409 Conflict si todavía hay ventas pendientes. */
    @PostMapping("/reiniciar")
    public Map<String, Boolean> reiniciar() {
        if (!ventaService.reiniciar()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hay ventas en curso; espera a que terminen para reiniciar");
        }
        return Map.of("reiniciado", true);
    }
}
