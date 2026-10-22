package co.tiendabarrio.dto.request;

import java.time.LocalDate;

import co.tiendabarrio.model.FormaPago;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;

/**
 * Pago de un ingreso al proveedor. Con formaPago se paga ya (efectivo, transferencia o tarjeta);
 * con formaPago CREDITO se acuerda pagar más adelante y fechaVencimiento es obligatoria.
 * En efectivo, montoEntregado (opcional) es lo que se le da al repartidor, para calcular su cambio.
 */
public record PagoProveedorRequest(
        FormaPago formaPago,
        @FutureOrPresent(message = "La fecha de vencimiento no puede estar en el pasado") LocalDate fechaVencimiento,
        @Min(value = 0, message = "El efectivo entregado no puede ser negativo") Long montoEntregado) {
}
