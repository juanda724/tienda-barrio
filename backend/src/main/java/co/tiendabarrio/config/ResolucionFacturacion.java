package co.tiendabarrio.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.tiendabarrio.exception.NegocioException;

/**
 * Resolución con la que la DIAN autoriza la numeración de las facturas electrónicas: un prefijo, un rango de
 * números y una fecha hasta la que se puede usar. En el prototipo es simulada (application.properties): el
 * sistema respeta sus límites, pero no la consulta ni la reporta a la DIAN.
 */
@Component
public class ResolucionFacturacion {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String numero;
    private final LocalDate fecha;
    private final String prefijo;
    private final long desde;
    private final long hasta;
    private final LocalDate vigenteHasta;
    private final String claveTecnica;

    /** Las fechas van en formato ISO (2026-01-01). */
    public ResolucionFacturacion(@Value("${tienda.factura.resolucion:18760000001}") String numero,
                                 @Value("${tienda.factura.resolucion-fecha:2026-01-01}") String fecha,
                                 @Value("${tienda.factura.prefijo:FE}") String prefijo,
                                 @Value("${tienda.factura.desde:1}") long desde,
                                 @Value("${tienda.factura.hasta:5000}") long hasta,
                                 @Value("${tienda.factura.vigente-hasta:2027-12-31}") String vigenteHasta,
                                 @Value("${tienda.factura.clave-tecnica:fc8eac422eba16e22ffd8c6f94b3f40a6e38162c}")
                                 String claveTecnica) {
        this.numero = numero;
        this.fecha = LocalDate.parse(fecha);
        this.prefijo = prefijo;
        this.desde = desde;
        this.hasta = hasta;
        this.vigenteHasta = LocalDate.parse(vigenteHasta);
        this.claveTecnica = claveTecnica;
    }

    /**
     * Consecutivo que le corresponde a la próxima factura: el siguiente al último usado, sin saltos, o el
     * primero del rango si aún no se ha facturado (ultimoUsado null).
     */
    public long siguiente(Long ultimoUsado) {
        return ultimoUsado == null ? desde : Math.max(ultimoUsado + 1, desde);
    }

    /** Rechaza facturar con una resolución vencida o con un número fuera del rango autorizado. */
    public void validarQueAutoriza(long consecutivo, LocalDate dia) {
        if (dia.isAfter(vigenteHasta)) {
            throw new NegocioException("La resolución de facturación " + numero + " venció el "
                    + vigenteHasta.format(FECHA) + ". Registre la venta sin factura electrónica");
        }
        if (consecutivo > hasta) {
            throw new NegocioException("Se agotó la numeración autorizada para facturas electrónicas ("
                    + rango() + "). Registre la venta sin factura electrónica");
        }
    }

    /** Número de la factura como se muestra: FE-12. */
    public String numero(long consecutivo) {
        return prefijo + "-" + consecutivo;
    }

    /** "Resolución DIAN 18760000001 del 01/01/2026. Numeración autorizada FE-1 a FE-5000, vigente hasta 31/12/2027". */
    public String descripcion() {
        return "Resolución DIAN " + numero + " del " + fecha.format(FECHA) + ". Numeración autorizada " + rango()
                + ", vigente hasta " + vigenteHasta.format(FECHA);
    }

    /** Clave que la DIAN entrega con la resolución y que entra en el cálculo del CUFE. */
    public String claveTecnica() {
        return claveTecnica;
    }

    private String rango() {
        return numero(desde) + " a " + numero(hasta);
    }
}
