package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Lugar;

import java.math.BigDecimal;

public record LugarDTO(
        Long lugarId,
        String nombre,
        String tipoLugar,
        String ciudad,
        BigDecimal latitud,
        BigDecimal longitud
) {
    public static LugarDTO desde(Lugar l) {
        return new LugarDTO(l.getLugarId(), l.getNombre(), l.getTipoLugar(), l.getCiudad(), l.getLatitud(), l.getLongitud());
    }
}
