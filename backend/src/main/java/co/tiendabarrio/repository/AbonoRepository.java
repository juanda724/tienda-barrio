package co.tiendabarrio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import co.tiendabarrio.model.Abono;

public interface AbonoRepository extends JpaRepository<Abono, Long> {

    List<Abono> findByClienteIdOrderByFechaHoraAscIdAsc(Long clienteId);

    @Query("select coalesce(sum(a.monto), 0) from Abono a where a.cliente.id = :clienteId")
    long totalAbonado(Long clienteId);
}
