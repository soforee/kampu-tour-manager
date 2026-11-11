package pe.edu.upeu.lp2_gp2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.lp2_gp2.entity.Proveedor;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    // Kampu Tours (propio) primero
    List<Proveedor> findAllByOrderByEsPropioDescProveedorIdAsc();

    Optional<Proveedor> findByRuc(String ruc);

    Optional<Proveedor> findFirstByEsPropio(Integer esPropio);

    // Teléfono repetido comparando solo dígitos (mismo criterio que el índice único). idExcluir = -1 al crear.
    @Query(value = "SELECT COUNT(*) FROM PROVEEDORES WHERE TRANSLATE(telefono, '0+-() ', '0') = :digitos AND proveedor_id <> :idExcluir", nativeQuery = true)
    long contarTelefono(@Param("digitos") String digitos, @Param("idExcluir") Long idExcluir);
}
