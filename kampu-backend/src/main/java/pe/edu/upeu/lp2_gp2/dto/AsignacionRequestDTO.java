package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotNull;

public record AsignacionRequestDTO(
        @NotNull(message = "Selecciona el vehículo.")
        Long vehiculoId,
        @NotNull(message = "Selecciona el conductor.")
        Long conductorId
) {
}
