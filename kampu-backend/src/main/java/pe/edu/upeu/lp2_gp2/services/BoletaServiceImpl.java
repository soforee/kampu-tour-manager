package pe.edu.upeu.lp2_gp2.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import pe.edu.upeu.lp2_gp2.entity.*;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class BoletaServiceImpl implements IBoletaService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final SalidaProgramadaRepository salidaRepository;
    private final PaqueteTourRepository paqueteRepository;
    private final GuiaRepository guiaRepository;
    private final PasajeroReservaRepository pasajeroRepository;
    private final PagoRepository pagoRepository;

    @Value("${spring.mail.username:}")
    private String remitente;

    public BoletaServiceImpl(JavaMailSender mailSender, ReservaRepository reservaRepository, ClienteRepository clienteRepository,
                             SalidaProgramadaRepository salidaRepository, PaqueteTourRepository paqueteRepository,
                             GuiaRepository guiaRepository, PasajeroReservaRepository pasajeroRepository,
                             PagoRepository pagoRepository) {
        this.mailSender = mailSender;
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
        this.salidaRepository = salidaRepository;
        this.paqueteRepository = paqueteRepository;
        this.guiaRepository = guiaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.pagoRepository = pagoRepository;
    }

    @Override
    public boolean envioHabilitado() {
        return remitente != null && !remitente.isBlank();
    }

    @Override
    @Transactional(readOnly = true)
    public String generarHtml(Long reservaId) {
        return construirHtml(datos(reservaId));
    }

    @Override
    @Transactional
    public String enviar(Long reservaId) {
        if (!envioHabilitado()) {
            throw new BusinessException("El envío de correos no está configurado en el servidor (MAIL_USERNAME / MAIL_PASSWORD).");
        }
        Datos d = datos(reservaId);
        if (d.email == null || d.email.isBlank()) {
            throw new BusinessException("El cliente no tiene correo registrado; no se puede enviar la boleta.");
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            // multipart = true: texto plano + HTML (los clientes de correo muestran el que soporten)
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "Kampu Tour Manager");
            helper.setTo(d.email);
            helper.setSubject("Tu reserva " + codigo(d.reserva) + " · " + d.paquete.getNombre());
            helper.setText(textoPlano(d), construirHtml(d));
            mailSender.send(mensaje);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException("No se pudo armar el correo: " + e.getMessage(), e);
        }
        d.reserva.setBoletaEnviadaEn(LocalDateTime.now());
        reservaRepository.save(d.reserva);
        return d.email;
    }

    // ---------- Datos de la boleta ----------

    private static class Datos {
        Reserva reserva;
        String nombre;
        String email;
        String telefono;
        SalidaProgramada salida;
        PaqueteTour paquete;
        Guia guia;
        List<PasajeroReserva> pasajeros;
        List<Pago> pagos;
    }

    private Datos datos(Long reservaId) {
        Datos d = new Datos();
        d.reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con Id: " + reservaId));
        if (d.reserva.getClienteId() != null) {
            Cliente c = clienteRepository.findById(d.reserva.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
            d.nombre = c.getNombreCompleto();
            d.email = c.getEmail();
            d.telefono = c.getTelefono();
        } else {
            d.nombre = d.reserva.getContactoNombre();
            d.email = d.reserva.getContactoEmail();
            d.telefono = d.reserva.getContactoTelefono();
        }
        d.salida = salidaRepository.findById(d.reserva.getSalidaId())
                .orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada."));
        d.paquete = paqueteRepository.findById(d.salida.getPaqueteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado."));
        d.guia = d.salida.getGuiaId() == null ? null : guiaRepository.findById(d.salida.getGuiaId()).orElse(null);
        d.pasajeros = pasajeroRepository.findByReservaIdOrderByPasajeroId(reservaId);
        d.pagos = pagoRepository.findByReservaIdAndEstadoTransaccion(reservaId, "APROBADO");
        return d;
    }

    // ---------- Formato ----------

    private String codigo(Reserva r) {
        return "R-" + String.format("%04d", r.getReservaId());
    }

    private String soles(BigDecimal monto) {
        return "S/ " + String.format(Locale.US, "%,.2f", monto == null ? BigDecimal.ZERO : monto);
    }

    private String fecha(LocalDate f) {
        return f == null ? "—" : f.format(FECHA);
    }

    private String esc(String s) {
        return s == null || s.isBlank() ? "—" : HtmlUtils.htmlEscape(s);
    }

    private String estadoTexto(String estado) {
        if (estado == null) return "";
        return estado.charAt(0) + estado.substring(1).toLowerCase();
    }

    private String textoPlano(Datos d) {
        BigDecimal pagado = d.reserva.getMontoPagado() == null ? BigDecimal.ZERO : d.reserva.getMontoPagado();
        return "Hola " + d.nombre + ",\n\n"
                + "Este es el resumen de tu reserva " + codigo(d.reserva) + ".\n"
                + "Tour: " + d.paquete.getNombre() + "\n"
                + "Fechas: " + fecha(d.salida.getFechaInicio()) + " al " + fecha(d.salida.getFechaFin()) + "\n"
                + "Viajeros: " + d.reserva.getNumeroViajeros() + "\n"
                + "Total: " + soles(d.reserva.getMontoTotal()) + "\n"
                + "Pagado: " + soles(pagado) + "\n"
                + "Saldo: " + soles(d.reserva.getMontoTotal().subtract(pagado).max(BigDecimal.ZERO)) + "\n\n"
                + "Documento informativo, no es un comprobante de pago tributario.\n"
                + "Kampu Tour Manager";
    }

    // HTML con estilos en línea y tablas: es lo que mejor soportan Gmail y Outlook.
    private String construirHtml(Datos d) {
        Reserva r = d.reserva;
        BigDecimal pagado = r.getMontoPagado() == null ? BigDecimal.ZERO : r.getMontoPagado();
        BigDecimal saldo = r.getMontoTotal().subtract(pagado).max(BigDecimal.ZERO);
        boolean pagadoTotal = saldo.signum() == 0;

        StringBuilder filasPasajeros = new StringBuilder();
        int i = 1;
        for (PasajeroReserva p : d.pasajeros) {
            filasPasajeros.append("<tr>")
                    .append(celda(String.valueOf(i++), "color:#A49D7B;width:28px;"))
                    .append(celda(esc(p.getNombreCompleto()), "font-weight:700;color:#2C3610;"))
                    .append(celda(esc(p.getTipoDocumento()) + " " + esc(p.getNumeroDocumento()), "color:#78735A;text-align:right;"))
                    .append("</tr>");
        }

        StringBuilder filasPagos = new StringBuilder();
        for (Pago p : d.pagos) {
            filasPagos.append("<tr>")
                    .append(celda(p.getFechaPago() == null ? "—" : p.getFechaPago().format(FECHA_HORA), "color:#78735A;"))
                    .append(celda(esc(p.getMetodoPago()), "color:#2C3610;"))
                    .append(celda(esc(p.getMoneda()) + " " + String.format(Locale.US, "%,.2f", p.getMonto()), "text-align:right;font-weight:700;color:#2C3610;"))
                    .append("</tr>");
        }

        String estadoColor = "CONFIRMADA".equals(r.getEstadoReserva()) || "COMPLETADA".equals(r.getEstadoReserva()) ? "#446825" : "#878126";
        String estadoFondo = "CONFIRMADA".equals(r.getEstadoReserva()) || "COMPLETADA".equals(r.getEstadoReserva()) ? "#EEF4E6" : "#FAF9E3";

        return """
                <!DOCTYPE html>
                <html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Boleta %s</title></head>
                <body style="margin:0;padding:0;background:#FBF6E4;font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#FBF6E4;padding:32px 12px;">
                <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;background:#FFFEF8;border:1px solid #E5DEC2;border-radius:20px;overflow:hidden;">
                  <tr><td style="background:#2C3610;padding:28px 32px;">
                    <table role="presentation" width="100%%"><tr>
                      <td style="font-size:24px;font-weight:900;color:#FDF8E6;letter-spacing:-0.5px;">Kampu</td>
                      <td align="right" style="font-size:11px;font-weight:800;letter-spacing:1.5px;text-transform:uppercase;color:#D7D068;">Boleta informativa</td>
                    </tr></table>
                    <div style="margin-top:22px;font-size:13px;color:#CED89C;">Reserva</div>
                    <div style="font-size:30px;font-weight:900;color:#FFFEF8;font-family:Consolas,monospace;">%s</div>
                    <span style="display:inline-block;margin-top:12px;padding:5px 12px;border-radius:999px;background:%s;color:%s;font-size:12px;font-weight:800;">%s</span>
                  </td></tr>

                  <tr><td style="padding:28px 32px 8px;">
                    <p style="margin:0;font-size:15px;color:#2C3610;">Hola <strong>%s</strong>,</p>
                    <p style="margin:8px 0 0;font-size:14px;line-height:1.6;color:#5B5741;">Gracias por viajar con nosotros. Este es el resumen de tu reserva.</p>
                  </td></tr>

                  <tr><td style="padding:16px 32px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#F4F6E4;border-radius:14px;">
                      <tr><td style="padding:18px 20px;">
                        <div style="font-size:11px;font-weight:800;letter-spacing:1.2px;text-transform:uppercase;color:#6B7E2B;">Tour</div>
                        <div style="margin-top:4px;font-size:18px;font-weight:800;color:#2C3610;">%s</div>
                        <table role="presentation" width="100%%" style="margin-top:14px;font-size:13px;color:#5B5741;">
                          <tr><td style="padding:3px 0;">Salida</td><td align="right" style="font-weight:700;color:#2C3610;">%s</td></tr>
                          <tr><td style="padding:3px 0;">Retorno</td><td align="right" style="font-weight:700;color:#2C3610;">%s</td></tr>
                          <tr><td style="padding:3px 0;">Duración</td><td align="right" style="font-weight:700;color:#2C3610;">%s días / %s noches</td></tr>
                          <tr><td style="padding:3px 0;">Guía</td><td align="right" style="font-weight:700;color:#2C3610;">%s</td></tr>
                        </table>
                      </td></tr>
                    </table>
                  </td></tr>

                  <tr><td style="padding:8px 32px;">
                    <div style="font-size:11px;font-weight:800;letter-spacing:1.2px;text-transform:uppercase;color:#78735A;padding-bottom:6px;">Pasajeros (%d)</div>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="font-size:13px;">%s</table>
                  </td></tr>

                  %s

                  <tr><td style="padding:16px 32px 8px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="font-size:14px;border-top:1px solid #E5DEC2;">
                      <tr><td style="padding:12px 0 4px;color:#78735A;">%d × %s</td><td align="right" style="padding:12px 0 4px;color:#2C3610;">%s</td></tr>
                      <tr><td style="padding:4px 0;color:#78735A;">Pagado</td><td align="right" style="padding:4px 0;color:#446825;font-weight:700;">%s</td></tr>
                      <tr><td style="padding:10px 0 0;font-size:16px;font-weight:800;color:#2C3610;">%s</td><td align="right" style="padding:10px 0 0;font-size:22px;font-weight:900;color:#2C3610;">%s</td></tr>
                    </table>
                  </td></tr>

                  <tr><td style="padding:20px 32px 28px;">
                    <p style="margin:0;font-size:12px;line-height:1.6;color:#A49D7B;">Reservado el %s · Contacto: %s · %s<br>
                    Documento informativo, no es un comprobante de pago tributario.</p>
                  </td></tr>
                </table>
                <p style="margin:18px 0 0;font-size:12px;color:#A49D7B;">Kampu Tour Manager</p>
                </td></tr></table>
                </body></html>
                """.formatted(
                codigo(r), codigo(r), estadoFondo, estadoColor, esc(estadoTexto(r.getEstadoReserva())),
                esc(d.nombre),
                esc(d.paquete.getNombre()), fecha(d.salida.getFechaInicio()), fecha(d.salida.getFechaFin()),
                d.paquete.getDias(), d.paquete.getNoches(), d.guia == null ? "Por asignar" : esc(d.guia.getNombreCompleto()),
                d.pasajeros.size(), filasPasajeros,
                d.pagos.isEmpty() ? "" : bloquePagos(filasPagos.toString()),
                r.getNumeroViajeros(), soles(d.paquete.getPrecioBase()), soles(r.getMontoTotal()),
                soles(pagado),
                pagadoTotal ? "Pagado por completo" : "Saldo pendiente", soles(saldo),
                r.getFechaReserva() == null ? "—" : r.getFechaReserva().format(FECHA_HORA), esc(d.email), esc(d.telefono));
    }

    private String bloquePagos(String filas) {
        return """
                <tr><td style="padding:16px 32px 8px;">
                  <div style="font-size:11px;font-weight:800;letter-spacing:1.2px;text-transform:uppercase;color:#78735A;padding-bottom:6px;">Pagos recibidos</div>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="font-size:13px;">%s</table>
                </td></tr>
                """.formatted(filas);
    }

    private String celda(String contenido, String estilo) {
        return "<td style=\"padding:8px 0;border-bottom:1px solid #F2EDD6;" + estilo + "\">" + contenido + "</td>";
    }
}
