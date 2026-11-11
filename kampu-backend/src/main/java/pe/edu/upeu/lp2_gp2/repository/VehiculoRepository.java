package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.Vehiculo;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findAllByOrderByVehiculoIdAsc();

    Optional<Vehiculo> findByPlacaIgnoreCase(String placa);

    boolean existsByProveedorId(Long proveedorId);
}
