package pe.edu.upeu.lp2_gp2.dto;

import java.math.BigDecimal;

// Vehículo o conductor que podría atender una operación; motivo = null si es elegible
public record RecursoCandidatoDTO(
        String tipoRecurso,
        Long recursoId,
        Long proveedorId,
        String proveedor,
        boolean proveedorPropio,
        String descripcion,
        Integer capacidadPax,
        BigDecimal horasDia,
        // null = elegible. advertencia: regla flexible (zona, servicio, preferencia) que solo se puede pasar por alto a mano
        String motivo,
        String advertencia
) {
}
