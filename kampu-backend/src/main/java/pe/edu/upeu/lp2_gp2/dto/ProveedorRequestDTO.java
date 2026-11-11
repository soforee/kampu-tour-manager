package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

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
        @Pattern(regexp = "ACTIVO|PENDIENTE|INACTIVO|SUSPENDIDO", message = "El estado debe ser ACTIVO, PENDIENTE, INACTIVO o SUSPENDIDO.")
        String estado,
        @Size(max = 200, message = "La dirección admite máximo 200 caracteres.")
        @Pattern(regexp = Reglas.TEXTO, message = "La dirección contiene caracteres no permitidos.")
        String direccion,
        @Size(max = 100, message = "El correo admite máximo 100 caracteres.")
        @Pattern(regexp = Reglas.EMAIL, message = "Escribe un correo válido (ejemplo: nombre@dominio.com).")
        String email,
        @Size(max = 150, message = "El contacto admite máximo 150 caracteres.")
        @Pattern(regexp = Reglas.TEXTO, message = "El contacto contiene caracteres no permitidos.")
        String contacto,
        // TRASLADO, TOUR, INTERPROVINCIAL
        @NotEmpty(message = "Elige al menos un servicio que ofrece la empresa.")
        List<@Pattern(regexp = "TRASLADO|TOUR|INTERPROVINCIAL", message = "Servicio no válido.") String> servicios,
        // Ciudades donde opera separadas por coma; vacío = todo el país
        @Size(max = 300, message = "Las zonas admiten máximo 300 caracteres.")
        @Pattern(regexp = Reglas.TEXTO, message = "Las zonas contienen caracteres no permitidos.")
        String zonas
) {
}
