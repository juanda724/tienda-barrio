package co.tiendabarrio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.IngresoMercancia;

public interface IngresoRepository extends JpaRepository<IngresoMercancia, Long> {

    List<IngresoMercancia> findAllByOrderByFechaHoraDescIdDesc();
}
