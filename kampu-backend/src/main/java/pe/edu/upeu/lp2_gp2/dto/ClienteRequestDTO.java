package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.*;

public record ClienteRequestDTO(
        @NotBlank(message = "Nombre y documento son obligatorios.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        // Persona (DNI/CE/PASAPORTE) o razón social (RUC); la regla de nombre de persona se aplica en el servicio
        @Pattern(regexp = Reglas.TEXTO, message = "El nombre contiene caracteres no permitidos.")
        String nombreCompleto,
        @NotNull(message = "El tipo de documento es obligatorio.")
        @Pattern(regexp = "DNI|CE|PASAPORTE|RUC", message = "El tipo de documento debe ser DNI, CE, PASAPORTE o RUC.")
        String tipoDocumento,
        @NotBlank(message = "Nombre y documento son obligatorios.")
        @Size(max = 20, message = "El documento admite máximo 20 caracteres.")
        String numeroDocumento,
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El email no es válido.")
        @Pattern(regexp = Reglas.EMAIL, message = "El email no es válido (ejemplo: nombre@dominio.com).")
        @Size(max = 100, message = "El email admite máximo 100 caracteres.")
        String email,
        // Con código de país: "+51 987654321". La cantidad de dígitos se valida en el servicio.
        @NotBlank(message = "El teléfono es obligatorio.")
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres.")
        @Pattern(regexp = Reglas.TELEFONO_PAIS, message = Reglas.MSG_TELEFONO_PAIS)
        String telefono
) {
}
