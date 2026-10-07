package co.tiendabarrio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.Devolucion;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    List<Devolucion> findAllByOrderByFechaHoraDescIdDesc();
}
