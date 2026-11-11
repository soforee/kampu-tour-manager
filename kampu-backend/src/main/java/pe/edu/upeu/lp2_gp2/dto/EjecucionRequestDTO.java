package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// Operador / Ventas: marcar una salida como en ejecución (CONFIRMADA) o terminada (COMPLETADA)
public record EjecucionRequestDTO(
        @NotNull(message = "Indica el estado.")
        @Pattern(regexp = "CONFIRMADA|COMPLETADA", message = "Solo se puede marcar CONFIRMADA o COMPLETADA.")
        String estado
) {
}
