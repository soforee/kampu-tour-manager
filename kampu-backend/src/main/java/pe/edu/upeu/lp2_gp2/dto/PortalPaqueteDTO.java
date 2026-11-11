package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.VistaPaqueteCliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Fila de VW_PAQUETES_CLIENTE: paquete del cliente con estado de ejecución y de pago
public record PortalPaqueteDTO(
        Long reservaId,
        Long salidaId,
        Long paqueteId,
        String paqueteNombre,
        String imagen,
        Integer dias,
        Integer noches,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer pasajeros,
        LocalDateTime fechaReserva,
        String estadoReserva,
        // POR_EMPEZAR / EN_CURSO / TERMINADO (CANCELADO)
        String estadoEjecucion,
        // PAGADO_COMPLETO / FALTA_PAGAR / PENDIENTE_PAGO (ANULADO)
        String estadoPago,
        BigDecimal montoTotal,
        BigDecimal montoPagado,
        BigDecimal saldo,
        BigDecimal montoPorValidar
) {
    public static PortalPaqueteDTO desde(VistaPaqueteCliente v) {
        return new PortalPaqueteDTO(v.getReservaId(), v.getSalidaId(), v.getPaqueteId(), v.getPaqueteNombre(),
                v.getImagen() == null ? "" : v.getImagen(), v.getDias(), v.getNoches(), v.getFechaInicio(), v.getFechaFin(),
                v.getPasajeros(), v.getFechaReserva(), v.getEstadoReserva(), v.getEstadoEjecucion(), v.getEstadoPago(),
                v.getMontoTotal(), v.getMontoPagado(), v.getSaldo(), v.getMontoPorValidar());
    }
}
