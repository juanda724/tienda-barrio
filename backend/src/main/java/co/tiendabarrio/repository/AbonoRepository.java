package co.tiendabarrio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.Abono;

public interface AbonoRepository extends JpaRepository<Abono, Long> {

    List<Abono> findByClienteIdOrderByFechaHoraDescIdDesc(Long clienteId);
}
