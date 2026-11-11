package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Auditoria;

import java.time.LocalDateTime;

// Fila de la bitácora para la sección "Auditoría" (solo ADMIN)
public record AuditoriaDTO(
        Long auditoriaId,
        String accion,
        String entidad,
        Long entidadId,
        String campo,
        String valorAnterior,
        String valorNuevo,
        String detalle,
        String usuario,
        String rolUsuario,
        String origen,
        LocalDateTime fecha
) {
    public static AuditoriaDTO desde(Auditoria a) {
        return new AuditoriaDTO(a.getAuditoriaId(), a.getAccion(), a.getEntidad(), a.getEntidadId(), a.getCampo(),
                a.getValorAnterior(), a.getValorNuevo(), a.getDetalle(), a.getUsuario(), a.getRolUsuario(),
                a.getOrigen(), a.getFecha());
    }
}
