package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.Pago;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByReservaIdOrderByPagoId(Long reservaId);
    List<Pago> findByReservaIdAndEstadoTransaccion(Long reservaId, String estadoTransaccion);

    List<Pago> findAllByOrderByPagoIdAsc();

    // Suma en soles (columna virtual monto_pen) de los pagos aprobados
    @Query("select coalesce(sum(p.montoPen), 0) from Pago p where p.reservaId = :reservaId and p.estadoTransaccion = 'APROBADO'")
    BigDecimal sumarAprobadosEnSoles(@Param("reservaId") Long reservaId);
}
