package co.tiendabarrio.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.AlertaStock;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {

    Optional<AlertaStock> findFirstByProductoIdAndResueltaIsNull(Long productoId);

    List<AlertaStock> findByResueltaIsNullOrderByCreadaDescIdDesc();
}
