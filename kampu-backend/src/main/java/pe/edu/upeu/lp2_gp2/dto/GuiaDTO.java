package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Guia;

public record GuiaDTO(
        Long guiaId,
        String nombreCompleto,
        String especialidad,
        String telefono
) {
    public static GuiaDTO desde(Guia g) {
        return new GuiaDTO(g.getGuiaId(), g.getNombreCompleto(), g.getEspecialidad() == null ? "" : g.getEspecialidad(), g.getTelefono() == null ? "" : g.getTelefono());
    }
}
