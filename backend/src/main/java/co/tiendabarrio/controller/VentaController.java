package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.ComprobanteResponse;
import co.tiendabarrio.dto.response.FacturaElectronicaResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.service.FacturaElectronicaService;
import co.tiendabarrio.service.VentaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventas;
    private final FacturaElectronicaService facturas;

    public VentaController(VentaService ventas, FacturaElectronicaService facturas) {
        this.ventas = ventas;
        this.facturas = facturas;
    }

    @GetMapping
    public List<VentaResponse> listar() {
        return ventas.listar();
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return ventas.obtener(id);
    }

    /** Comprobante de la venta; con telefono, el enlace de WhatsApp va dirigido a ese cliente. */
    @GetMapping("/{id}/comprobante")
    public ComprobanteResponse comprobante(@PathVariable Long id, @RequestParam(required = false) String telefono) {
        return ventas.comprobante(id, telefono);
    }

    /** Factura electrónica de la venta; error si el cliente no la pidió. */
    @GetMapping("/{id}/factura-electronica")
    public FacturaElectronicaResponse facturaElectronica(@PathVariable Long id) {
        return facturas.generar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse registrar(@Valid @RequestBody VentaRequest datos) {
        return ventas.registrar(datos);
    }
}
