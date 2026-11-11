package pe.edu.upeu.lp2_gp2.dto;

import java.util.List;

// Resultado de la asignación automática de todas las operaciones pendientes
public record AsignacionMasivaDTO(
        int revisadas,
        List<OperacionDTO> asignadas,
        // "KMP-013: motivo" de las que no encontraron vehículo y conductor
        List<String> sinAsignar
) {
}
