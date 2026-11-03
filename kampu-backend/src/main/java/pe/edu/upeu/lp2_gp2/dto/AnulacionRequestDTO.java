package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.Size;

public record AnulacionRequestDTO(
        @Size(max = 50, message = "La forma de reembolso admite máximo 50 caracteres.")
        String formaReembolso
) {
}
