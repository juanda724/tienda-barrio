package co.tiendabarrio.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.FacturaElectronicaRequest;
import co.tiendabarrio.dto.response.AdquirienteResponse;
import co.tiendabarrio.model.Adquiriente;
import co.tiendabarrio.model.TipoDocumento;
import co.tiendabarrio.repository.AdquirienteRepository;

/** Clientes que piden factura electrónica, guardados por su documento para reutilizar sus datos. */
@Service
public class AdquirienteService {

    private final AdquirienteRepository adquirientes;

    public AdquirienteService(AdquirienteRepository adquirientes) {
        this.adquirientes = adquirientes;
    }

    /** Datos guardados de un cliente por su documento, para llenar el formulario de factura electrónica. */
    @Transactional(readOnly = true)
    public Optional<AdquirienteResponse> buscar(TipoDocumento tipo, String numero) {
        return adquirientes.findByTipoDocumentoAndNumeroDocumento(tipo, normalizar(numero)).map(AdquirienteResponse::de);
    }

    /** Busca al cliente por su documento (o lo crea) y guarda los datos que trae la venta. */
    @Transactional
    public Adquiriente guardar(FacturaElectronicaRequest datos) {
        String numero = normalizar(datos.numeroDocumento());
        Adquiriente adquiriente = adquirientes.findByTipoDocumentoAndNumeroDocumento(datos.tipoDocumento(), numero)
                .orElseGet(() -> new Adquiriente(datos.tipoDocumento(), numero));
        adquiriente.actualizar(datos.nombre().trim(), datos.correo().trim(), limpiar(datos.telefono()),
                limpiar(datos.direccion()), limpiar(datos.ciudad()));
        return adquirientes.save(adquiriente);
    }

    /** "1.023.456.789" y "1023456789" son el mismo documento. */
    private static String normalizar(String numero) {
        return numero == null ? "" : numero.replaceAll("[\\s.]", "").toUpperCase();
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
