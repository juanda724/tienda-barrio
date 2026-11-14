package co.tiendabarrio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Datos con los que la tienda se identifica como emisor de sus facturas electrónicas (SWR-32). Se definen en
 * application.properties; los valores por defecto son de ejemplo.
 */
@Component
public class DatosEmisor {

    private final String nombre;
    private final String razonSocial;
    private final String nit;
    private final String direccion;
    private final String telefono;
    private final boolean responsableIva;

    public DatosEmisor(@Value("${tienda.nombre:Tienda de Barrio}") String nombre,
                       @Value("${tienda.razon-social:Tienda de Barrio}") String razonSocial,
                       @Value("${tienda.nit:123456789-6}") String nit,
                       @Value("${tienda.direccion:}") String direccion,
                       @Value("${tienda.telefono:}") String telefono,
                       @Value("${tienda.responsable-iva:false}") boolean responsableIva) {
        this.nombre = nombre;
        this.razonSocial = razonSocial;
        this.nit = nit;
        this.direccion = direccion.isBlank() ? null : direccion.trim();
        this.telefono = telefono.isBlank() ? null : telefono.trim();
        this.responsableIva = responsableIva;
    }

    /** Nombre comercial, el que encabeza los documentos. */
    public String nombre() {
        return nombre;
    }

    public String razonSocial() {
        return razonSocial;
    }

    /** NIT con su dígito de verificación, como se muestra: 123456789-6. */
    public String nit() {
        return nit;
    }

    /** NIT sin dígito de verificación ni separadores, como entra en el cálculo del CUFE: 123456789. */
    public String nitSinDigito() {
        return nit.split("-")[0].replaceAll("\\D", "");
    }

    /** Dirección del establecimiento; null si no se configuró. */
    public String direccion() {
        return direccion;
    }

    /** Teléfono del establecimiento; null si no se configuró. */
    public String telefono() {
        return telefono;
    }

    /**
     * true si la tienda es responsable de IVA: sus ventas guardan el tratamiento de cada producto y sus
     * facturas discriminan el impuesto (SWR-33). Con false, las facturas dicen "No responsable de IVA".
     */
    public boolean responsableIva() {
        return responsableIva;
    }
}
