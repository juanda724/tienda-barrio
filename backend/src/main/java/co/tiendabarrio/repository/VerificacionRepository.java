package co.tiendabarrio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.VerificacionIngreso;

public interface VerificacionRepository extends JpaRepository<VerificacionIngreso, Long> {

    List<VerificacionIngreso> findByIngresoIdOrderByFechaHoraDescIdDesc(Long ingresoId);
}
