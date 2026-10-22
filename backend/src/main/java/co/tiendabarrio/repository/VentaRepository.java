package co.tiendabarrio.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findAllByOrderByFechaHoraDescIdDesc();

    List<Venta> findByClienteIdAndFormaPagoOrderByFechaHoraAscIdAsc(Long clienteId, FormaPago formaPago);

    /** Ventas entre dos instantes (desde incluido, hasta excluido), para los reportes. */
    List<Venta> findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(LocalDateTime desde,
                                                                                       LocalDateTime hasta);

    /** Suma de las ventas a crédito de un cliente: lo que se le ha fiado en total. */
    @Query("select coalesce(sum(v.total), 0) from Venta v "
            + "where v.cliente.id = :clienteId and v.formaPago = co.tiendabarrio.model.FormaPago.CREDITO")
    long totalFiado(Long clienteId);
}
