package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// Afiliación web de una empresa de transporte: crea la empresa (PENDIENTE) y su usuario PROVEEDOR
public record AfiliacionRequestDTO(
        @NotBlank(message = "Ingresa el nombre comercial de la empresa.")
        @Size(max = 150, message = "El nombre comercial admite máximo 150 caracteres.")
        @Pattern(regexp = Reglas.TEXTO, message = "El nombre comercial contiene caracteres no permitidos.")
        String nombreComercial,
        @NotBlank(message = "Ingresa el RUC de la empresa.")
        @Pattern(regexp = Reglas.RUC, message = Reglas.MSG_RUC)
        String ruc,
        @NotBlank(message = "Ingresa el teléfono de la empresa.")
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres.")
        @Pattern(regexp = Reglas.TELEFONO_PAIS, message = Reglas.MSG_TELEFONO_PAIS)
        String telefono,
        @NotBlank(message = "Ingresa el correo de la empresa.")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres.")
        @Pattern(regexp = Reglas.EMAIL, message = "Escribe un correo válido (ejemplo: nombre@dominio.com).")
        String email,
        @NotBlank(message = "Ingresa la dirección de la empresa.")
        @Size(max = 200, message = "La dirección admite máximo 200 caracteres.")
        @Pattern(regexp = Reglas.TEXTO, message = "La dirección contiene caracteres no permitidos.")
        String direccion,
        @NotBlank(message = "Ingresa la persona de contacto.")
        @Size(max = 150, message = "El contacto admite máximo 150 caracteres.")
        @Pattern(regexp = Reglas.NOMBRE_PERSONA, message = "El contacto " + Reglas.MSG_NOMBRE_PERSONA)
        String contacto,
        @NotEmpty(message = "Elige al menos un servicio que ofrece la empresa.")
        List<@Pattern(regexp = "TRASLADO|TOUR|INTERPROVINCIAL", message = "Servicio no válido.") String> servicios,
        @Size(max = 300, message = "Las zonas admiten máximo 300 caracteres.")
        @Pattern(regexp = Reglas.TEXTO, message = "Las zonas contienen caracteres no permitidos.")
        String zonas,
        @NotBlank(message = "Elige un usuario para la empresa.")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{4,30}$", message = "El usuario tiene de 4 a 30 caracteres: letras, números y . _ -")
        String username,
        @NotBlank(message = "Ingresa una contraseña.")
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres.")
        String password,
        @NotBlank(message = "Confirma tu contraseña.")
        String confirmacion
) {
}
