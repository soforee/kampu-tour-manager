package pe.edu.upeu.lp2_gp2.services;

// Se publica al confirmar una reserva o aprobar un pago.
// BoletaListener envía la boleta cuando la transacción ya se guardó (AFTER_COMMIT).
public record BoletaEvento(Long reservaId, String motivo) {
}
