package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorRequestDTO(
        @NotBlank(message = "El nombre comercial es obligatorio.")
        @Size(max = 150, message = "El nombre comercial admite máximo 150 caracteres.")
        @Pattern(regexp = Reglas.TEXTO, message = "El nombre comercial contiene caracteres no permitidos.")
        String nombreComercial,
        @NotBlank(message = "El RUC es obligatorio.")
        @Pattern(regexp = Reglas.RUC, message = Reglas.MSG_RUC)
        String ruc,
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres.")
        @Pattern(regexp = Reglas.TELEFONO, message = Reglas.MSG_TELEFONO)
        String telefono,
        // true: Kampu Tours (solo puede haber uno)
        boolean esPropio,
        @NotNull(message = "El estado es obligatorio.")
        @Pattern(regexp = "ACTIVO|INACTIVO|SUSPENDIDO", message = "El estado debe ser ACTIVO, INACTIVO o SUSPENDIDO.")
        String estado
) {
}
