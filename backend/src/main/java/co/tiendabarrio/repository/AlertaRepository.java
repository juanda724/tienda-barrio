package co.tiendabarrio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.Alerta;
import co.tiendabarrio.model.EstadoAlerta;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    boolean existsByProductoIdAndEstado(Long productoId, EstadoAlerta estado);

    List<Alerta> findByProductoIdAndEstado(Long productoId, EstadoAlerta estado);

    List<Alerta> findByEstadoOrderByFechaHoraDescIdDesc(EstadoAlerta estado);

    List<Alerta> findAllByOrderByFechaHoraDescIdDesc();
}
