package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.PaqueteTour;

import java.math.BigDecimal;

public record PaqueteTourDTO(
        Long paqueteId,
        String nombre,
        String descripcion,
        BigDecimal precioBase,
        Integer dias,
        Integer noches,
        String imagen,
        String estado
) {
    public static PaqueteTourDTO desde(PaqueteTour p) {
        return new PaqueteTourDTO(p.getPaqueteId(), p.getNombre(), p.getDescripcion() == null ? "" : p.getDescripcion(), p.getPrecioBase(),
                p.getDias(), p.getNoches(), p.getImagen() == null ? "" : p.getImagen(), p.getEstado());
    }
}
