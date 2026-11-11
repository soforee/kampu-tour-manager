package pe.edu.upeu.lp2_gp2.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.Reserva;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    boolean existsByClienteId(Long clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.reservaId = :id")
    Optional<Reserva> findByIdParaActualizar(@Param("id") Long id);

    List<Reserva> findAllByOrderByReservaIdAsc();
}
