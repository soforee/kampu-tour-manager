package pe.edu.upeu.lp2_gp2.dto;

import java.time.LocalDateTime;

public record HistorialOperacionDTO(
        Long historialId,
        Long operacionId,
        String estadoAnterior,
        String estadoNuevo,
        LocalDateTime fechaCambio,
        String usuarioRegistro,
        String observacion
) {
}
