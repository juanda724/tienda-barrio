package co.tiendabarrio.proveedor;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngresoRepository extends JpaRepository<IngresoMercancia, Long> {

    List<IngresoMercancia> findAllByOrderByFechaHoraDescIdDesc();
}
