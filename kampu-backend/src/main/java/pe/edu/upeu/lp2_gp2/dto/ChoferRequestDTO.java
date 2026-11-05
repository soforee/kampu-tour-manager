package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChoferRequestDTO(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        @Pattern(regexp = Reglas.NOMBRE_PERSONA, message = "El nombre " + Reglas.MSG_NOMBRE_PERSONA)
        String nombreCompleto,
        @NotBlank(message = "El número de licencia es obligatorio.")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{6,20}\\s*$", message = "La licencia solo admite letras y números (6 a 20 caracteres).")
        String numeroLicencia,
        @NotNull(message = "La categoría de licencia es obligatoria.")
        @Pattern(regexp = "A-IIa|A-IIb|A-IIIa|A-IIIb|A-IIIc", message = "Categoría de licencia no válida.")
        String categoriaLicencia,
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres.")
        @Pattern(regexp = Reglas.TELEFONO, message = Reglas.MSG_TELEFONO)
        String telefono,
        Long transporteId,
        @NotNull(message = "El estado es obligatorio.")
        @Pattern(regexp = "ACTIVO|INACTIVO", message = "El estado debe ser ACTIVO o INACTIVO.")
        String estado
) {
}
