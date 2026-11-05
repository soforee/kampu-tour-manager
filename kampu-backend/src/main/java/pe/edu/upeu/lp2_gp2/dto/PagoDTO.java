package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoDTO(
        Long pagoId,
        Long reservaId,
        BigDecimal monto,
        String moneda,
        String metodoPago,
        String codigoTransaccion,
        LocalDateTime fechaPago,
        String estadoTransaccion
) {
    public static PagoDTO desde(Pago p) {
        return new PagoDTO(p.getPagoId(), p.getReservaId(), p.getMonto(), p.getMoneda(), p.getMetodoPago() == null ? "" : p.getMetodoPago(),
                p.getCodigoTransaccion() == null ? "" : p.getCodigoTransaccion(), p.getFechaPago(), p.getEstadoTransaccion());
    }
}
