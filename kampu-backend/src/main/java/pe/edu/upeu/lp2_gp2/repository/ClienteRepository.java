package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.Cliente;

import java.util.List;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByOrderByClienteIdAsc();

    // Autocompletado: coincide por nombre, documento o correo (sin distinguir mayúsculas).
    // :texto llega ya en mayúsculas, con % al inicio y al final y los comodines (% _ !) escapados con '!'.
    @Query("select c from Cliente c " +
            "where upper(c.nombreCompleto) like :texto escape '!' " +
            "or upper(c.numeroDocumento) like :texto escape '!' " +
            "or upper(c.email) like :texto escape '!' " +
            "order by c.nombreCompleto")
    List<Cliente> buscar(@Param("texto") String texto, Pageable pagina);

    // Teléfono repetido comparando solo los dígitos (mismo criterio que el índice único uq_cli_telefono).
    // idExcluir = -1 al crear.
    @Query(value = "SELECT COUNT(*) FROM CLIENTES WHERE TRANSLATE(telefono, '0+-() ', '0') = :digitos AND cliente_id <> :idExcluir",
            nativeQuery = true)
    long contarTelefono(@Param("digitos") String digitos, @Param("idExcluir") Long idExcluir);

    // El número de documento es único sin importar el tipo (uq_cli_numero_doc)
    Optional<Cliente> findFirstByNumeroDocumentoIgnoreCase(String numeroDocumento);

    Optional<Cliente> findFirstByEmailIgnoreCase(String email);
}
