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

@RestController
@RequestMapping("/api")
public class VentaController {

    public record VentaRequest(String cliente, int productoId, int cantidad) {}

    private final VentaService ventaService;
    private final InventarioService inventarioService;

    public VentaController(VentaService ventaService, InventarioService inventarioService) {
        this.ventaService = ventaService;
        this.inventarioService = inventarioService;
    }

    @GetMapping("/productos")
    public List<Producto> productos() {
        return inventarioService.listar();
    }

    @GetMapping("/ventas")
    public List<Venta> ventas() {
        return ventaService.listar();
    }

    @GetMapping("/resumen")
    public Resumen resumen() {
        return ventaService.resumen();
    }

    @PostMapping("/ventas")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Venta registrar(@RequestBody VentaRequest req) {
        if (req.cliente() == null || req.cliente().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente es obligatorio");
        }
        if (req.cantidad() < 1 || req.cantidad() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe estar entre 1 y 100");
        }
        return ventaService.registrar(req.cliente().trim(), req.productoId(), req.cantidad());
    }

    @PostMapping("/ventas/simular")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Integer> simular(@RequestParam(defaultValue = "20") int cantidad) {
        if (cantidad < 1 || cantidad > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad entre 1 y 100");
        }
        return Map.of("generadas", ventaService.simular(cantidad));
    }

    @PostMapping("/reiniciar")
    public Map<String, Boolean> reiniciar() {
        if (!ventaService.reiniciar()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hay ventas en curso; espera a que terminen para reiniciar");
        }
        return Map.of("reiniciado", true);
    }
}
