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
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    List<Cliente> findAllByOrderByClienteIdAsc();

    // Autocompletado: coincide por nombre, documento o correo (sin distinguir mayúsculas).
    // :texto llega ya en mayúsculas, con % al inicio y al final y los comodines (% _ !) escapados con '!'.
    @Query("select c from Cliente c " +
            "where upper(c.nombreCompleto) like :texto escape '!' " +
            "or upper(c.numeroDocumento) like :texto escape '!' " +
            "or upper(c.email) like :texto escape '!' " +
            "order by c.nombreCompleto")
    List<Cliente> buscar(@Param("texto") String texto, Pageable pagina);
}
