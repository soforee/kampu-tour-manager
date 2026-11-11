package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.HistorialOperacion;

import java.util.List;

@Repository
public interface HistorialOperacionRepository extends JpaRepository<HistorialOperacion, Long> {
    List<HistorialOperacion> findByOperacionIdOrderByHistorialIdAsc(Long operacionId);

    List<HistorialOperacion> findAllByOrderByHistorialIdAsc();
}
