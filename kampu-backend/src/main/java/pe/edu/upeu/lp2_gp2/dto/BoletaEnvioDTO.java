package pe.edu.upeu.lp2_gp2.dto;

import java.time.LocalDateTime;

public record BoletaEnvioDTO(
        String destinatario,
        LocalDateTime enviadaEn
) {
}
