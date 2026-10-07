package co.tiendabarrio.dto.request;

import java.time.LocalDate;

import co.tiendabarrio.model.FormaPago;
import jakarta.validation.constraints.FutureOrPresent;

/**
 * Pago de un ingreso al proveedor. Con formaPago se paga ya (efectivo, transferencia o tarjeta);
 * con formaPago CREDITO se acuerda pagar más adelante y fechaVencimiento es obligatoria.
 */
public record PagoProveedorRequest(
        FormaPago formaPago,
        @FutureOrPresent(message = "La fecha de vencimiento no puede estar en el pasado") LocalDate fechaVencimiento) {
}
