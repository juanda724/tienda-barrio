package co.tiendabarrio.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.tiendabarrio.model.Adquiriente;
import co.tiendabarrio.model.TipoDocumento;

public interface AdquirienteRepository extends JpaRepository<Adquiriente, Long> {

    Optional<Adquiriente> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
