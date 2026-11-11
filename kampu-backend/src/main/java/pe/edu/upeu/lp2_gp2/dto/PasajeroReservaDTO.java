package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.PasajeroReserva;

import java.time.LocalDate;

public record PasajeroReservaDTO(
        Long pasajeroId,
        Long detalleId,
        Long reservaId,
        String nombreCompleto,
        String tipoDocumento,
        String numeroDocumento,
        String nacionalidad,
        LocalDate fechaNacimiento
) {
    public static PasajeroReservaDTO desde(PasajeroReserva p, Long reservaId) {
        return new PasajeroReservaDTO(p.getPasajeroId(), p.getDetalleId(), reservaId, p.getNombreCompleto(), p.getTipoDocumento(),
                p.getNumeroDocumento(), p.getNacionalidad() == null ? "" : p.getNacionalidad(), p.getFechaNacimiento());
    }
}
