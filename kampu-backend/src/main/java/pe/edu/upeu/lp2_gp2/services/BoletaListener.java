package pe.edu.upeu.lp2_gp2.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BoletaListener {
    private final IBoletaService boletaService;

    public BoletaListener(IBoletaService boletaService) {
        this.boletaService = boletaService;
    }

    // Solo si la confirmación/pago se guardó; en otro hilo para no demorar la respuesta.
    // Si el correo falla, la reserva no se ve afectada: solo se registra el error.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmar(BoletaEvento evento) {
        try {
            if (!boletaService.envioHabilitado()) return;
            String destino = boletaService.enviar(evento.reservaId());
            System.out.println("Boleta de la reserva " + evento.reservaId() + " (" + evento.motivo() + ") enviada a " + destino);
        } catch (Exception e) {
            System.err.println("No se pudo enviar la boleta de la reserva " + evento.reservaId() + ": " + e.getMessage());
        }
    }
}
