package pe.edu.upeu.lp2_gp2.dto;

import java.math.BigDecimal;

// Duración de una ruta calculada con datos reales.
// fuente: OSRM (red vial de OpenStreetMap) o ESTIMADO (PKG_RUTAS con las coordenadas de los lugares)
public record EstimacionDuracionDTO(
        Integer minutos,
        BigDecimal distanciaKm,
        String fuente,
        String detalle
) {
}
