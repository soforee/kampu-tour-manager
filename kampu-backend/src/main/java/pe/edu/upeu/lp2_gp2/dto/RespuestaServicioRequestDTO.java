package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// La empresa proveedora acepta (confirma) o rechaza el servicio que se le asignó
public record RespuestaServicioRequestDTO(
        @NotNull(message = "Indica si aceptas o rechazas el servicio.")
        Boolean aceptar,
        @Size(max = 200, message = "El motivo admite máximo 200 caracteres.")
        String motivo
) {
}
