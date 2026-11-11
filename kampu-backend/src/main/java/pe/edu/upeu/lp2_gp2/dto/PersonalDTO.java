package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Personal;

import java.time.LocalDate;

public record PersonalDTO(
        Long personalId,
        Long proveedorId,
        String nombreCompleto,
        String rol,
        String telefono,
        String numLicencia,
        String catLicencia,
        LocalDate vencLicencia,
        String estado
) {
    public static PersonalDTO desde(Personal p) {
        return new PersonalDTO(p.getPersonalId(), p.getProveedorId(), p.getNombreCompleto(), p.getRol(),
                p.getTelefono() == null ? "" : p.getTelefono(), p.getNumLicencia(), p.getCatLicencia(), p.getVencLicencia(),
                p.getEstado());
    }
}
