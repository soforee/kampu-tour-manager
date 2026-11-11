package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// "Registrarse": autoregistro de un cliente externo (SP_REGISTRAR_CLIENTE_EXTERNO)
public record RegistroRequestDTO(
        @NotBlank(message = "Ingresa tu nombre completo.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        @Pattern(regexp = Reglas.NOMBRE_PERSONA, message = "El nombre " + Reglas.MSG_NOMBRE_PERSONA)
        String nombreCompleto,
        @NotNull(message = "Elige el tipo de documento.")
        @Pattern(regexp = "DNI|CE|PASAPORTE", message = "El tipo de documento debe ser DNI, CE o PASAPORTE.")
        String tipoDocumento,
        @NotBlank(message = "Ingresa tu número de documento.")
        @Size(max = 20, message = "El documento admite máximo 20 caracteres.")
        String numeroDocumento,
        @NotBlank(message = "Ingresa tu correo.")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres.")
        @Pattern(regexp = Reglas.EMAIL, message = "Escribe un correo válido (ejemplo: nombre@dominio.com).")
        String email,
        @NotBlank(message = "Ingresa tu teléfono.")
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres.")
        @Pattern(regexp = Reglas.TELEFONO_PAIS, message = Reglas.MSG_TELEFONO_PAIS)
        String telefono,
        @NotBlank(message = "Ingresa una contraseña.")
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres.")
        String password,
        @NotBlank(message = "Confirma tu contraseña.")
        String confirmacion
) {
}
