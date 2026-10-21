package co.tiendabarrio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.TipoPago;
import co.tiendabarrio.model.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findAllByOrderByFechaHoraDescIdDesc();

    List<Venta> findByClienteIdAndTipoPagoOrderByFechaHoraDescIdDesc(Long clienteId, TipoPago tipoPago);
}
