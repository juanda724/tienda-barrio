package co.tiendabarrio.service;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.PedidoProveedor;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.util.Enlaces;

/**
 * Arma el mensaje del pedido para el proveedor y los enlaces que abren WhatsApp o el correo
 * con ese mensaje ya escrito. El dueño revisa el mensaje y lo envía desde su propia cuenta.
 */
@Service
public class MensajePedidoService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String nombreTienda;

    public MensajePedidoService(@Value("${tienda.nombre:Tienda de Barrio}") String nombreTienda) {
        this.nombreTienda = nombreTienda;
    }

    public String mensaje(PedidoProveedor pedido) {
        Proveedor proveedor = pedido.getProveedor();
        String saludo = proveedor.getNombreContacto() != null ? proveedor.getNombreContacto() : proveedor.getNombre();
        StringBuilder texto = new StringBuilder()
                .append("Hola ").append(saludo).append(", le escribe ").append(nombreTienda).append(".\n\n")
                .append("Quisiéramos hacer el siguiente pedido (Pedido #").append(pedido.getId()).append("):\n");
        for (LineaProducto linea : pedido.getLineas()) {
            texto.append("- ").append(linea.getProducto().getNombre()).append(": ").append(linea.getCantidad())
                    .append('\n');
        }
        if (pedido.getFechaEstimadaEntrega() != null) {
            texto.append("\nFecha deseada de entrega: ").append(pedido.getFechaEstimadaEntrega().format(FECHA))
                    .append('\n');
        }
        if (pedido.getObservaciones() != null) {
            texto.append("Observaciones: ").append(pedido.getObservaciones()).append('\n');
        }
        return texto.append("\nPor favor confírmenos la disponibilidad y la fecha de entrega. ¡Gracias!").toString();
    }

    /** Enlace de WhatsApp con el mensaje; null si el proveedor no tiene teléfono. */
    public String whatsappUrl(PedidoProveedor pedido, String mensaje) {
        String telefono = pedido.getProveedor().getTelefono();
        return telefono == null || telefono.isBlank() ? null : Enlaces.whatsapp(telefono, mensaje);
    }

    /** Enlace de Gmail con asunto y mensaje; null si el proveedor no tiene correo. */
    public String correoUrl(PedidoProveedor pedido, String mensaje) {
        return Enlaces.correo(pedido.getProveedor().getCorreo(), "Pedido #" + pedido.getId() + " - " + nombreTienda,
                mensaje);
    }
}
