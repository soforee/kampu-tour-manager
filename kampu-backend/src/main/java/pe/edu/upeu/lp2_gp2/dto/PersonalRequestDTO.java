package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Guía, conductor o ambos. La licencia es obligatoria para quien conduce (chk_pers_licencia).
public record PersonalRequestDTO(
        @NotNull(message = "Selecciona el proveedor (Kampu Tours si es personal propio).")
        Long proveedorId,
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        @Pattern(regexp = Reglas.NOMBRE_PERSONA, message = "El nombre " + Reglas.MSG_NOMBRE_PERSONA)
        String nombreCompleto,
        @NotNull(message = "El rol es obligatorio.")
        @Pattern(regexp = "GUIA|CONDUCTOR|AMBOS", message = "El rol debe ser GUIA, CONDUCTOR o AMBOS.")
        String rol,
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres.")
        @Pattern(regexp = Reglas.TELEFONO, message = Reglas.MSG_TELEFONO)
        String telefono,
        @Pattern(regexp = "^$|^\\s*[A-Za-z0-9]{6,20}\\s*$", message = "La licencia solo admite letras y números (6 a 20 caracteres).")
        String numLicencia,
        @Pattern(regexp = "^$|A-I|A-IIa|A-IIb|A-IIIa|A-IIIb|A-IIIc", message = "Categoría de licencia no válida.")
        String catLicencia,
        LocalDate vencLicencia,
        @NotNull(message = "El estado es obligatorio.")
        @Pattern(regexp = "DISPONIBLE|OCUPADO|INACTIVO", message = "El estado debe ser DISPONIBLE, OCUPADO o INACTIVO.")
        String estado
) {
}
