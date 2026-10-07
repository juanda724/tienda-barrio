package co.tiendabarrio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.model.Proveedor;

public interface IngresoRepository extends JpaRepository<IngresoMercancia, Long> {

    List<IngresoMercancia> findAllByOrderByFechaHoraDescIdDesc();

    /** Proveedores que han entregado el producto, del ingreso más reciente al más antiguo. */
    @Query("select i.proveedor from IngresoMercancia i join i.lineas l "
            + "where l.producto.id = :productoId order by i.fechaHora desc, i.id desc")
    List<Proveedor> proveedoresQueEntregaron(Long productoId);
}
