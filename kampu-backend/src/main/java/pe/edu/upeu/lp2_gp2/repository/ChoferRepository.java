package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.Chofer;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChoferRepository extends JpaRepository<Chofer, Long> {
    List<Chofer> findAllByOrderByChoferIdAsc();

    List<Chofer> findByTransporteId(Long transporteId);

    Optional<Chofer> findByNumeroLicenciaIgnoreCase(String numeroLicencia);
}
