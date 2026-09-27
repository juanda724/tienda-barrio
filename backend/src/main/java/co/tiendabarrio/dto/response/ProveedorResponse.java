package co.tiendabarrio.dto.response;

import java.util.List;

import co.tiendabarrio.model.Proveedor;

public record ProveedorResponse(
        Long id,
        String nombre,
        String nombreContacto,
        String telefono,
        String correo,
        List<ProductoResponse> productos) {

    public static ProveedorResponse de(Proveedor p) {
        return new ProveedorResponse(p.getId(), p.getNombre(), p.getNombreContacto(), p.getTelefono(),
                p.getCorreo(), p.getProductos().stream().map(ProductoResponse::de).toList());
    }
}
