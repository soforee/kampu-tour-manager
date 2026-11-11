package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.DetalleReserva;

import java.util.List;

@Repository
public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, Long> {
    List<DetalleReserva> findAllByOrderByDetalleIdAsc();

    List<DetalleReserva> findByReservaIdOrderByDetalleIdAsc(Long reservaId);

    boolean existsBySalidaId(Long salidaId);

    List<DetalleReserva> findBySalidaId(Long salidaId);
}
