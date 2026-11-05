package pe.edu.upeu.lp2_gp2.services;

public interface IBoletaService {
    boolean envioHabilitado();
    String generarHtml(Long reservaId);
    // Envía la boleta y devuelve el correo de destino
    String enviar(Long reservaId);
}
