package co.tiendabarrio.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import co.tiendabarrio.model.EstadoPedido;
import co.tiendabarrio.model.PedidoProveedor;

public interface PedidoProveedorRepository extends JpaRepository<PedidoProveedor, Long> {

    List<PedidoProveedor> findAllByOrderByFechaCreacionDescIdDesc();

    List<PedidoProveedor> findByEstadoOrderByFechaCreacionDescIdDesc(EstadoPedido estado);

    /** Pedidos en alguno de los estados dados que incluyen el producto, del más reciente al más antiguo. */
    @Query("select distinct p from PedidoProveedor p join p.lineas l "
            + "where l.producto.id = :productoId and p.estado in :estados order by p.fechaCreacion desc")
    List<PedidoProveedor> conProductoEnEstados(Long productoId, Collection<EstadoPedido> estados);
}
