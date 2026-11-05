package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Reserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaDTO(
        Long reservaId,
        Long clienteId,
        Long salidaId,
        LocalDateTime fechaReserva,
        Integer numeroViajeros,
        BigDecimal montoTotal,
        BigDecimal montoPagado,
        String estadoReserva,
        String estadoPago,
        String observaciones,
        String formaReembolso,
        Long usuarioAnulaId,
        String contactoNombre,
        String contactoEmail,
        String contactoTelefono,
        LocalDateTime boletaEnviadaEn
) {
    public static ReservaDTO desde(Reserva r) {
        return new ReservaDTO(r.getReservaId(), r.getClienteId(), r.getSalidaId(), r.getFechaReserva(),
                r.getNumeroViajeros(), r.getMontoTotal(), r.getMontoPagado() == null ? BigDecimal.ZERO : r.getMontoPagado(),
                r.getEstadoReserva(), r.getEstadoPago(), r.getObservaciones() == null ? "" : r.getObservaciones(), r.getFormaReembolso(), r.getUsuarioAnulaId(),
                r.getContactoNombre(), r.getContactoEmail(), r.getContactoTelefono(), r.getBoletaEnviadaEn());
    }
}
