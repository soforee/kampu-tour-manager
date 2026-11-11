package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.SalidaProgramada;

import java.time.LocalDate;

public record SalidaProgramadaDTO(
        Long salidaId,
        Long paqueteId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer cuposMaximos,
        Integer cuposDisponibles,
        Long guiaId,
        String estado,
        // Operaciones de transporte vigentes de la salida
        Integer operaciones,
        // PKG_ASIGNACION.faltante_transporte: null si el transporte alcanza para los pasajeros vendidos
        String faltanteTransporte
) {
    public static SalidaProgramadaDTO desde(SalidaProgramada s, Integer operaciones, String faltante) {
        return new SalidaProgramadaDTO(s.getSalidaId(), s.getPaqueteId(), s.getFechaInicio(), s.getFechaFin(),
                s.getCuposMaximos(), s.getCuposDisponibles(), s.getGuiaId(), s.getEstado(),
                operaciones == null ? 0 : operaciones, faltante);
    }
}
