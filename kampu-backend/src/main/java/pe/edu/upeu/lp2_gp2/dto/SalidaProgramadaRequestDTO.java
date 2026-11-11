package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

// La fecha fin no se envía: el backend la calcula con los días del paquete.
// El transporte no va en la salida: se asigna por tramo en OPERACIONES_TRANSPORTE.
public record SalidaProgramadaRequestDTO(
        @NotNull(message = "Selecciona un paquete.")
        Long paqueteId,
        @NotNull(message = "La fecha de inicio es obligatoria.")
        LocalDate fechaInicio,
        @NotNull(message = "Los cupos máximos son obligatorios.")
        @Min(value = 1, message = "Los cupos máximos deben estar entre 1 y 999.")
        @Max(value = 999, message = "Los cupos máximos deben estar entre 1 y 999.")
        Integer cuposMaximos,
        Long guiaId,
        @NotNull(message = "El estado es obligatorio.")
        @Pattern(regexp = "PROGRAMADA|CONFIRMADA|COMPLETADA|CANCELADA", message = "Estado de salida no válido.")
        String estado
) {
}
