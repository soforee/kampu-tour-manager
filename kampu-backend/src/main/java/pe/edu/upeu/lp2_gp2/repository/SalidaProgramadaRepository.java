package pe.edu.upeu.lp2_gp2.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.SalidaProgramada;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalidaProgramadaRepository extends JpaRepository<SalidaProgramada, Long> {
    boolean existsByPaqueteId(Long paqueteId);
    boolean existsByGuiaId(Long guiaId);
    boolean existsByTransporteId(Long transporteId);
    boolean existsByChoferId(Long choferId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SalidaProgramada s where s.salidaId = :id")
    Optional<SalidaProgramada> findByIdParaActualizar(@Param("id") Long id);

    List<SalidaProgramada> findByGuiaIdAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            Long guiaId, List<String> estados, LocalDate fin, LocalDate inicio);

    List<SalidaProgramada> findByTransporteIdAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            Long transporteId, List<String> estados, LocalDate fin, LocalDate inicio);

    List<SalidaProgramada> findByChoferIdAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            Long choferId, List<String> estados, LocalDate fin, LocalDate inicio);

    List<SalidaProgramada> findAllByOrderBySalidaIdAsc();
}
