package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Chofer;

public record ChoferDTO(
        Long choferId,
        String nombreCompleto,
        String numeroLicencia,
        String categoriaLicencia,
        String telefono,
        Long transporteId,
        String estado
) {
    public static ChoferDTO desde(Chofer c) {
        return new ChoferDTO(c.getChoferId(), c.getNombreCompleto(), c.getNumeroLicencia(), c.getCategoriaLicencia(),
                c.getTelefono() == null ? "" : c.getTelefono(), c.getTransporteId(), c.getEstado());
    }
}
