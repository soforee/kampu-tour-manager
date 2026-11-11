package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.VistaPaqueteCliente;

import java.util.List;

// Vista VW_PAQUETES_CLIENTE: ya viene filtrada por el cliente de la sesión de base de datos
@Repository
public interface VistaPaqueteClienteRepository extends JpaRepository<VistaPaqueteCliente, Long> {
    List<VistaPaqueteCliente> findAllByOrderByFechaInicioDesc();

    List<VistaPaqueteCliente> findByReservaId(Long reservaId);
}
