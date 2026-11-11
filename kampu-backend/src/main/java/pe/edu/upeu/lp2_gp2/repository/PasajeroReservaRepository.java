package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.PasajeroReserva;

import java.util.Collection;
import java.util.List;

@Repository
public interface PasajeroReservaRepository extends JpaRepository<PasajeroReserva, Long> {
    List<PasajeroReserva> findByDetalleIdInOrderByPasajeroIdAsc(Collection<Long> detalleIds);

    List<PasajeroReserva> findAllByOrderByPasajeroIdAsc();

    // Mismo documento en otra reserva activa de la misma salida (trg_pasajeros_doc_salida)
    @Query(value = """
            SELECT COUNT(*)
              FROM PASAJEROS_RESERVA p
              JOIN DETALLE_RESERVA d ON d.detalle_id = p.detalle_id
              JOIN RESERVAS r        ON r.reserva_id = d.reserva_id
             WHERE d.salida_id = :salidaId
               AND r.estado_reserva <> 'ANULADA'
               AND p.tipo_documento = :tipo
               AND UPPER(p.numero_documento) = UPPER(:numero)""", nativeQuery = true)
    long contarEnSalida(@Param("salidaId") Long salidaId, @Param("tipo") String tipo, @Param("numero") String numero);
}
