package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Datos mínimos de un cliente no registrado (invitado). Se guardan en la propia reserva.
public record ContactoReservaDTO(
        @NotBlank(message = "Ingresa el nombre del contacto.")
        @Size(max = 150, message = "El nombre del contacto admite máximo 150 caracteres.")
        @Pattern(regexp = Reglas.NOMBRE_PERSONA, message = "El nombre del contacto " + Reglas.MSG_NOMBRE_PERSONA)
        String nombre,
        @NotBlank(message = "Ingresa el correo del contacto.")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres.")
        @Pattern(regexp = Reglas.EMAIL, message = "El correo del contacto no es válido.")
        String email,
        @NotBlank(message = "Ingresa el teléfono del contacto.")
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres.")
        @Pattern(regexp = Reglas.TELEFONO, message = Reglas.MSG_TELEFONO)
        String telefono
) {
}
