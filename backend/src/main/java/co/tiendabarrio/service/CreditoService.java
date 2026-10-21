package co.tiendabarrio.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.AbonoRequest;
import co.tiendabarrio.dto.response.AbonoResponse;
import co.tiendabarrio.dto.response.ClienteCreditoResponse;
import co.tiendabarrio.dto.response.ClienteDetalleResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.Abono;
import co.tiendabarrio.model.Cliente;
import co.tiendabarrio.model.TipoPago;
import co.tiendabarrio.model.Venta;
import co.tiendabarrio.repository.AbonoRepository;
import co.tiendabarrio.repository.ClienteRepository;
import co.tiendabarrio.repository.VentaRepository;
import co.tiendabarrio.util.Dinero;

/**
 * Ventas a crédito (fiado) por cliente (F-08).
 * saldo pendiente = total de ventas a crédito del cliente − total de abonos (SWR-22).
 */
@Service
public class CreditoService {

    private final ClienteRepository clientes;
    private final AbonoRepository abonos;
    private final VentaRepository ventas;

    public CreditoService(ClienteRepository clientes, AbonoRepository abonos, VentaRepository ventas) {
        this.clientes = clientes;
        this.abonos = abonos;
        this.ventas = ventas;
    }

    /** Busca al cliente por nombre (sin importar mayúsculas) y, si no existe, lo crea (SWR-21). */
    @Transactional
    public Cliente obtenerOCrearCliente(String nombre, String telefono) {
        String limpio = nombre.trim();
        Cliente cliente = clientes.findByNombreIgnoreCase(limpio).orElseGet(() -> clientes.save(new Cliente(limpio)));
        if (telefono != null && !telefono.isBlank()) {
            cliente.setTelefono(telefono.trim());
        }
        return cliente;
    }

    /** Clientes con su saldo pendiente: primero los que más deben. */
    @Transactional(readOnly = true)
    public List<ClienteCreditoResponse> listarClientes() {
        return clientes.findAllByOrderByNombreAsc().stream()
                .map(this::resumen)
                .sorted(Comparator.comparing(ClienteCreditoResponse::saldoPendiente).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteDetalleResponse detalle(Long clienteId) {
        Cliente cliente = buscar(clienteId);
        List<VentaResponse> ventasACredito = ventas
                .findByClienteIdAndTipoPagoOrderByFechaHoraDescIdDesc(clienteId, TipoPago.CREDITO).stream()
                .map(VentaResponse::de).toList();
        List<AbonoResponse> historial = abonos.findByClienteIdOrderByFechaHoraDescIdDesc(clienteId).stream()
                .map(AbonoResponse::de).toList();
        return new ClienteDetalleResponse(resumen(cliente), ventasACredito, historial);
    }

    /** Registra un abono. No se acepta más de lo que el cliente debe (SWR-23). */
    @Transactional
    public ClienteCreditoResponse registrarAbono(Long clienteId, AbonoRequest datos) {
        Cliente cliente = buscar(clienteId);
        BigDecimal saldo = resumen(cliente).saldoPendiente();
        BigDecimal monto = Dinero.redondear(datos.monto());
        if (saldo.signum() <= 0) {
            throw new NegocioException(cliente.getNombre() + " no tiene saldo pendiente");
        }
        if (monto.compareTo(saldo) > 0) {
            throw new NegocioException("El abono (" + Dinero.formatear(monto) + ") supera el saldo pendiente ("
                    + Dinero.formatear(saldo) + ")");
        }
        String nota = datos.nota() == null || datos.nota().isBlank() ? null : datos.nota().trim();
        abonos.save(new Abono(cliente, monto, nota));
        return resumen(cliente);
    }

    private Cliente buscar(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NoEncontradoException("Cliente no encontrado"));
    }

    private ClienteCreditoResponse resumen(Cliente cliente) {
        BigDecimal fiado = ventas
                .findByClienteIdAndTipoPagoOrderByFechaHoraDescIdDesc(cliente.getId(), TipoPago.CREDITO).stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal abonado = abonos.findByClienteIdOrderByFechaHoraDescIdDesc(cliente.getId()).stream()
                .map(Abono::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ClienteCreditoResponse(cliente.getId(), cliente.getNombre(), cliente.getTelefono(), fiado,
                abonado, fiado.subtract(abonado));
    }
}
