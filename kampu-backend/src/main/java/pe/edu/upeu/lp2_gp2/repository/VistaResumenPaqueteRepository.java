package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.VistaResumenPaquete;

import java.util.List;

// Vista VW_RESUMEN_PAQUETES_POR_ROL: filas y montos según el rol de la sesión de base de datos
@Repository
public interface VistaResumenPaqueteRepository extends JpaRepository<VistaResumenPaquete, Long> {
    List<VistaResumenPaquete> findAllByOrderByFechaInicioAsc();
}
