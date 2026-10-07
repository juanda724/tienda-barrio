package co.tiendabarrio.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.AbonoRequest;
import co.tiendabarrio.dto.request.ClienteRequest;
import co.tiendabarrio.dto.response.ClienteResponse;
import co.tiendabarrio.dto.response.EstadoCuentaResponse;
import co.tiendabarrio.dto.response.EstadoCuentaResponse.Movimiento;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.Abono;
import co.tiendabarrio.model.Cliente;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.Venta;
import co.tiendabarrio.repository.AbonoRepository;
import co.tiendabarrio.repository.ClienteRepository;
import co.tiendabarrio.repository.VentaRepository;
import co.tiendabarrio.util.Dinero;
import co.tiendabarrio.util.Enlaces;

/**
 * Clientes y sus cuentas de fiado (F-08): saldo pendiente (SWR-22) y abonos hasta saldar la
 * deuda (SWR-23). Las ventas a crédito se registran en VentaService (SWR-21).
 */
@Service
public class ClienteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClienteRepository clientes;
    private final VentaRepository ventas;
    private final AbonoRepository abonos;
    private final String nombreTienda;

    public ClienteService(ClienteRepository clientes, VentaRepository ventas, AbonoRepository abonos,
                          @Value("${tienda.nombre:Tienda de Barrio}") String nombreTienda) {
        this.clientes = clientes;
        this.ventas = ventas;
        this.abonos = abonos;
        this.nombreTienda = nombreTienda;
    }

    /** Clientes con su saldo; primero los que más deben. */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clientes.findAllByOrderByNombreAsc().stream()
                .map(this::resumen)
                .sorted(Comparator.comparingLong(ClienteResponse::saldoPendiente).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return resumen(buscar(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest datos) {
        String nombre = datos.nombre().trim();
        if (clientes.existsByNombreIgnoreCase(nombre)) {
            throw new NegocioException("Ya existe un cliente llamado " + nombre);
        }
        return resumen(clientes.save(new Cliente(nombre, limpiar(datos.telefono()), limpiar(datos.direccion()))));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest datos) {
        Cliente cliente = buscar(id);
        String nombre = datos.nombre().trim();
        if (clientes.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NegocioException("Ya existe un cliente llamado " + nombre);
        }
        cliente.setNombre(nombre);
        cliente.setTelefono(limpiar(datos.telefono()));
        cliente.setDireccion(limpiar(datos.direccion()));
        return resumen(cliente);
    }

    /** Lo que el cliente debe hoy: ventas a crédito menos abonos (SWR-22). */
    @Transactional(readOnly = true)
    public long saldo(Long clienteId) {
        return ventas.totalFiado(clienteId) - abonos.totalAbonado(clienteId);
    }

    /** Ventas fiadas y abonos en orden de fecha, con el saldo acumulado después de cada uno. */
    @Transactional(readOnly = true)
    public EstadoCuentaResponse estadoCuenta(Long id) {
        Cliente cliente = buscar(id);
        List<Movimiento> sinSaldo = new ArrayList<>();
        for (Venta v : ventas.findByClienteIdAndFormaPagoOrderByFechaHoraAscIdAsc(id, FormaPago.CREDITO)) {
            sinSaldo.add(new Movimiento("VENTA", v.getId(), v.getFechaHora(), describir(v), v.getTotal(), 0, 0));
        }
        for (Abono a : abonos.findByClienteIdOrderByFechaHoraAscIdAsc(id)) {
            String descripcion = "Abono en " + a.getFormaPago().getNombre().toLowerCase()
                    + (a.getNota() == null ? "" : " · " + a.getNota());
            sinSaldo.add(new Movimiento("ABONO", a.getId(), a.getFechaHora(), descripcion, 0, a.getMonto(), 0));
        }
        sinSaldo.sort(Comparator.comparing(Movimiento::fechaHora));

        List<Movimiento> movimientos = new ArrayList<>();
        long saldo = 0;
        for (Movimiento m : sinSaldo) {
            saldo += m.cargo() - m.abono();
            movimientos.add(new Movimiento(m.tipo(), m.numero(), m.fechaHora(), m.descripcion(), m.cargo(),
                    m.abono(), saldo));
        }

        ClienteResponse resumen = resumen(cliente);
        String recordatorio = recordatorio(cliente, resumen.saldoPendiente(), movimientos);
        String whatsapp = cliente.getTelefono() == null ? null : Enlaces.whatsapp(cliente.getTelefono(), recordatorio);
        return new EstadoCuentaResponse(resumen, movimientos, recordatorio, whatsapp);
    }

    /** Registra un abono; no puede superar lo que el cliente debe (SWR-23). */
    @Transactional
    public EstadoCuentaResponse registrarAbono(Long id, AbonoRequest datos) {
        Cliente cliente = buscar(id);
        if (datos.formaPago() == FormaPago.CREDITO) {
            throw new NegocioException("Un abono no puede hacerse a crédito");
        }
        long saldo = saldo(id);
        if (saldo <= 0) {
            throw new NegocioException(cliente.getNombre() + " no tiene saldo pendiente");
        }
        if (datos.monto() > saldo) {
            throw new NegocioException("El abono (" + Dinero.formatear(datos.monto())
                    + ") supera el saldo pendiente (" + Dinero.formatear(saldo) + ")");
        }
        String nota = datos.nota() == null || datos.nota().isBlank() ? null : datos.nota().trim();
        abonos.save(new Abono(cliente, datos.monto(), datos.formaPago(), nota));
        return estadoCuenta(id);
    }

    Cliente buscar(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NoEncontradoException("Cliente no encontrado"));
    }

    private ClienteResponse resumen(Cliente cliente) {
        return ClienteResponse.de(cliente, ventas.totalFiado(cliente.getId()), abonos.totalAbonado(cliente.getId()));
    }

    private static String describir(Venta venta) {
        int unidades = venta.getLineas().stream().mapToInt(l -> l.getCantidad()).sum();
        String primero = venta.getLineas().isEmpty() ? "" : venta.getLineas().get(0).getProducto().getNombre();
        int otros = venta.getLineas().size() - 1;
        return primero + (otros > 0 ? " y " + otros + " más" : "") + " (" + unidades
                + (unidades == 1 ? " unidad)" : " unidades)");
    }

    private String recordatorio(Cliente cliente, long saldo, List<Movimiento> movimientos) {
        StringBuilder texto = new StringBuilder("Hola ").append(cliente.getNombre()).append(", le escribe ")
                .append(nombreTienda).append(".\n\n");
        if (saldo <= 0) {
            return texto.append("Su cuenta está al día. ¡Gracias por su compra!").toString();
        }
        texto.append("Le recordamos que su saldo pendiente es de ").append(Dinero.formatear(saldo)).append(".\n");
        movimientos.stream().filter(m -> m.tipo().equals("ABONO")).reduce((a, b) -> b).ifPresent(ultimo ->
                texto.append("Su último abono fue el ").append(ultimo.fechaHora().format(FECHA)).append(" por ")
                        .append(Dinero.formatear(ultimo.abono())).append(".\n"));
        return texto.append("\nPuede abonar en la tienda cuando le quede fácil. ¡Gracias!").toString();
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
