package co.tiendabarrio.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNombreIgnoreCase(String nombre);

    List<Cliente> findAllByOrderByNombreAsc();
}
