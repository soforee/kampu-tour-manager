package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoDTO(
        Long pagoId,
        Long reservaId,
        BigDecimal monto,
        String moneda,
        BigDecimal tipoCambio,
        BigDecimal montoPen,
        String metodoPago,
        String codigoTransaccion,
        LocalDateTime fechaPago,
        String estadoTransaccion,
        // ADMIN / VENDEDOR / CLIENTE (informado desde el portal)
        String registradoPor
) {
    public static PagoDTO desde(Pago p) {
        return new PagoDTO(p.getPagoId(), p.getReservaId(), p.getMonto(), p.getMoneda(), p.getTipoCambio(), p.getMontoPen(),
                p.getMetodoPago() == null ? "" : p.getMetodoPago(),
                p.getCodigoTransaccion() == null ? "" : p.getCodigoTransaccion(), p.getFechaPago(), p.getEstadoTransaccion(),
                p.getRegistradoPor());
    }
}
