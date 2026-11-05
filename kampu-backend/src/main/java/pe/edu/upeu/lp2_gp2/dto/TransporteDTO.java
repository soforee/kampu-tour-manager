package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Transporte;

public record TransporteDTO(
        Long transporteId,
        String placa,
        String marcaModelo,
        Integer capacidad
) {
    public static TransporteDTO desde(Transporte t) {
        return new TransporteDTO(t.getTransporteId(), t.getPlaca(), t.getMarcaModelo() == null ? "" : t.getMarcaModelo(), t.getCapacidad());
    }
}
