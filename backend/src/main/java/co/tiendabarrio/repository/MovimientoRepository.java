package co.tiendabarrio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.MovimientoInventario;

public interface MovimientoRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findAllByOrderByFechaHoraDescIdDesc();

    List<MovimientoInventario> findByProductoIdOrderByFechaHoraDescIdDesc(Long productoId);
}
