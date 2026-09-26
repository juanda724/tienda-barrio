package co.tiendabarrio.inventario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findAllByOrderByFechaHoraDescIdDesc();

    List<MovimientoInventario> findByProductoIdOrderByFechaHoraDescIdDesc(Long productoId);
}
