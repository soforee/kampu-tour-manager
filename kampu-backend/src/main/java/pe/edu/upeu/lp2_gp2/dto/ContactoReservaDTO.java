package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Cliente nuevo que reserva sin estar registrado: en v4 toda reserva tiene cliente, así que
// se crea en CLIENTES (sin cuenta web; luego puede activarla con "Registrarse").
public record ContactoReservaDTO(
        @NotBlank(message = "Ingresa el nombre del cliente.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        @Pattern(regexp = Reglas.NOMBRE_PERSONA, message = "El nombre " + Reglas.MSG_NOMBRE_PERSONA)
        String nombre,
        @NotNull(message = "El tipo de documento del cliente es obligatorio.")
        @Pattern(regexp = "DNI|CE|PASAPORTE", message = "El tipo de documento debe ser DNI, CE o PASAPORTE.")
        String tipoDocumento,
        @NotBlank(message = "Ingresa el número de documento del cliente.")
        @Size(max = 20, message = "El documento admite máximo 20 caracteres.")
        String numeroDocumento,
        @NotBlank(message = "Ingresa el correo del cliente.")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres.")
        @Pattern(regexp = Reglas.EMAIL, message = "El correo del cliente no es válido.")
        String email,
        @NotBlank(message = "Ingresa el teléfono del cliente.")
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres.")
        @Pattern(regexp = Reglas.TELEFONO_PAIS, message = Reglas.MSG_TELEFONO_PAIS)
        String telefono
) {
}
