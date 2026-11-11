package pe.edu.upeu.lp2_gp2.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class BoletaServiceImpl implements IBoletaService {
    private static final Locale ES = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEEE", ES);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Logo (PNG, porque Gmail y Outlook no muestran SVG). En el correo va como imagen en línea (cid).
    private static final String LOGO = "boleta/logo-kampu.png";
    private static final String LOGO_CID = "logoKampu";

    // Datos de contacto de la agencia que aparecen al pie de la boleta
    private static final String AGENCIA_EMAIL = "reservas@kampu.pe";
    private static final String AGENCIA_TELEFONO = "+51 984 000 000";
    private static final String AGENCIA_CIUDAD = "Cusco, Perú";

    private final JavaMailSender mailSender;
    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final SalidaProgramadaRepository salidaRepository;
    private final PaqueteTourRepository paqueteRepository;
    private final PersonalRepository guiaRepository;
    private final PasajeroReservaRepository pasajeroRepository;
    private final DetalleReservaRepository detalleRepository;
    private final PagoRepository pagoRepository;

    private String logoBase64;

    @Value("${spring.mail.username:}")
    private String remitente;

    public BoletaServiceImpl(JavaMailSender mailSender, ReservaRepository reservaRepository, ClienteRepository clienteRepository,
                             SalidaProgramadaRepository salidaRepository, PaqueteTourRepository paqueteRepository,
                             PersonalRepository guiaRepository, PasajeroReservaRepository pasajeroRepository,
                             DetalleReservaRepository detalleRepository, PagoRepository pagoRepository) {
        this.mailSender = mailSender;
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
        this.salidaRepository = salidaRepository;
        this.paqueteRepository = paqueteRepository;
        this.guiaRepository = guiaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.detalleRepository = detalleRepository;
        this.pagoRepository = pagoRepository;
    }

    @Override
    public boolean envioHabilitado() {
        return remitente != null && !remitente.isBlank();
    }

    // Vista previa en el navegador: logo incrustado y botón para imprimir / guardar como PDF
    @Override
    @Transactional(readOnly = true)
    public String generarHtml(Long reservaId) {
        return construirHtml(datos(reservaId), "data:image/png;base64," + logoBase64(), true);
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
            // multipart = true: texto plano + HTML + logo en línea
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "Kampu Tour Manager");
            helper.setTo(d.email);
            helper.setSubject("Tu reserva " + codigo(d.reserva) + " · " + d.paquete.getNombre());
            helper.setText(textoPlano(d), construirHtml(d, "cid:" + LOGO_CID, false));
            helper.addInline(LOGO_CID, new ClassPathResource(LOGO)); // logo-kampu.png, image/png
            mailSender.send(mensaje);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException("No se pudo armar el correo: " + e.getMessage(), e);
        }
        d.reserva.setBoletaEnviadaEn(LocalDateTime.now());
        reservaRepository.saveAndFlush(d.reserva);
        return d.email;
    }

    private String logoBase64() {
        if (logoBase64 == null) {
            try (InputStream in = new ClassPathResource(LOGO).getInputStream()) {
                logoBase64 = Base64.getEncoder().encodeToString(in.readAllBytes());
            } catch (IOException e) {
                throw new IllegalStateException("No se encontró el logo de la boleta (" + LOGO + ").", e);
            }
        }
        return logoBase64;
    }

    // ---------- Datos de la boleta ----------

    private static class Datos {
        Reserva reserva;
        String nombre;
        String documento;
        String email;
        String telefono;
        DetalleReserva detalle;
        int viajeros;
        SalidaProgramada salida;
        PaqueteTour paquete;
        Personal guia;
        List<PasajeroReserva> pasajeros;
        List<Pago> pagos;
    }

    private Datos datos(Long reservaId) {
        Datos d = new Datos();
        d.reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con Id: " + reservaId));
        // v4: toda reserva tiene cliente
        Cliente c = clienteRepository.findById(d.reserva.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
        d.nombre = c.getNombreCompleto();
        d.documento = c.getTipoDocumento() + " " + c.getNumeroDocumento();
        d.email = c.getEmail();
        d.telefono = c.getTelefono();
        // La aplicación registra una salida por reserva (un DETALLE_RESERVA); se toma el primero
        List<DetalleReserva> detalles = detalleRepository.findByReservaIdOrderByDetalleIdAsc(reservaId);
        if (detalles.isEmpty()) throw new ResourceNotFoundException("La reserva no tiene salidas reservadas.");
        d.detalle = detalles.get(0);
        d.viajeros = detalles.stream().mapToInt(DetalleReserva::getCantidad).sum();
        d.salida = salidaRepository.findById(d.detalle.getSalidaId())
                .orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada."));
        d.paquete = paqueteRepository.findById(d.salida.getPaqueteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado."));
        d.guia = d.salida.getGuiaId() == null ? null : guiaRepository.findById(d.salida.getGuiaId()).orElse(null);
        d.pasajeros = pasajeroRepository.findByDetalleIdInOrderByPasajeroIdAsc(
                detalles.stream().map(DetalleReserva::getDetalleId).toList());
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

    private String fechaLarga(LocalDate f) {
        return f == null ? "—" : f.format(FECHA_LARGA);
    }

    private String diaSemana(LocalDate f) {
        if (f == null) return "";
        String dia = f.format(DIA_SEMANA);
        return dia.substring(0, 1).toUpperCase(ES) + dia.substring(1);
    }

    private String esc(String s) {
        return s == null || s.isBlank() ? "—" : HtmlUtils.htmlEscape(s);
    }

    private String estadoTexto(String estado) {
        if (estado == null) return "";
        return estado.charAt(0) + estado.substring(1).toLowerCase();
    }

    private BigDecimal pagado(Reserva r) {
        return r.getMontoPagado() == null ? BigDecimal.ZERO : r.getMontoPagado();
    }

    private String textoPlano(Datos d) {
        BigDecimal pagado = pagado(d.reserva);
        return "Hola " + d.nombre + ",\n\n"
                + "Este es el resumen de tu reserva " + codigo(d.reserva) + ".\n"
                + "Tour: " + d.paquete.getNombre() + "\n"
                + "Salida: " + fechaLarga(d.salida.getFechaInicio()) + "\n"
                + "Retorno: " + fechaLarga(d.salida.getFechaFin()) + "\n"
                + "Viajeros: " + d.viajeros + "\n"
                + "Total: " + soles(d.reserva.getMontoTotal()) + "\n"
                + "Pagado: " + soles(pagado) + "\n"
                + "Saldo: " + soles(d.reserva.getMontoTotal().subtract(pagado).max(BigDecimal.ZERO)) + "\n\n"
                + "Documento informativo, no es un comprobante de pago tributario.\n"
                + "Kampu Tour Manager · " + AGENCIA_EMAIL + " · " + AGENCIA_TELEFONO;
    }

    // HTML con estilos en línea y tablas: es lo que mejor soportan Gmail y Outlook.
    private String construirHtml(Datos d, String srcLogo, boolean navegador) {
        Reserva r = d.reserva;
        BigDecimal pagado = pagado(r);
        BigDecimal saldo = r.getMontoTotal().subtract(pagado).max(BigDecimal.ZERO);
        boolean pagadoTotal = saldo.signum() == 0;
        int avance = r.getMontoTotal().signum() == 0 ? 0
                : pagado.multiply(BigDecimal.valueOf(100)).divide(r.getMontoTotal(), 0, RoundingMode.DOWN).intValue();
        avance = Math.max(0, Math.min(100, avance));

        StringBuilder filasPasajeros = new StringBuilder();
        int i = 1;
        for (PasajeroReserva p : d.pasajeros) {
            filasPasajeros.append("<tr>")
                    .append(celda(String.valueOf(i++), "color:#A49D7B;width:28px;"))
                    .append(celda(esc(p.getNombreCompleto()), "font-weight:700;color:#2C3610;"))
                    .append(celda(esc(p.getTipoDocumento()) + " " + esc(p.getNumeroDocumento()), "color:#5B5741;font-family:Consolas,Menlo,monospace;font-size:12px;"))
                    .append(celda(esc(p.getNacionalidad()), "color:#78735A;text-align:right;"))
                    .append("</tr>");
        }

        StringBuilder filasPagos = new StringBuilder();
        for (Pago p : d.pagos) {
            filasPagos.append("<tr>")
                    .append(celda(p.getFechaPago() == null ? "—" : p.getFechaPago().format(FECHA_HORA), "color:#5B5741;"))
                    .append(celda(esc(p.getMetodoPago()), "color:#2C3610;"))
                    .append(celda(esc(p.getCodigoTransaccion()), "color:#78735A;font-family:Consolas,Menlo,monospace;font-size:12px;"))
                    .append(celda(esc(p.getMoneda()) + " " + String.format(Locale.US, "%,.2f", p.getMonto()), "text-align:right;font-weight:700;color:#2C3610;white-space:nowrap;"))
                    .append("</tr>");
        }

        boolean confirmada = "CONFIRMADA".equals(r.getEstadoReserva()) || "COMPLETADA".equals(r.getEstadoReserva());
        String pillReserva = pill("Reserva " + estadoTexto(r.getEstadoReserva()).toLowerCase(),
                confirmada ? "#EEF4E6" : "#FAF9E3", confirmada ? "#36521E" : "#666120");
        String pillPago = pill(pagadoTotal ? "Pagado" : (pagado.signum() > 0 ? "Pago parcial" : "Pago pendiente"),
                pagadoTotal ? "#EEF4E6" : "#F7F0E8", pagadoTotal ? "#36521E" : "#5A3D28");

        String barraAvance = avance <= 0
                ? "<td style=\"background:#E5DEC2;height:8px;border-radius:999px;font-size:0;line-height:0;\">&nbsp;</td>"
                : avance >= 100
                ? "<td style=\"background:#4B5B1D;height:8px;border-radius:999px;font-size:0;line-height:0;\">&nbsp;</td>"
                : "<td width=\"" + avance + "%\" style=\"background:#4B5B1D;height:8px;border-radius:999px 0 0 999px;font-size:0;line-height:0;\">&nbsp;</td>"
                + "<td style=\"background:#E5DEC2;height:8px;border-radius:0 999px 999px 0;font-size:0;line-height:0;\">&nbsp;</td>";

        String barraNavegador = navegador ? """
                <div class="no-print" style="max-width:640px;margin:0 auto 16px;text-align:right;">
                  <button onclick="window.print()" style="font:700 14px 'Segoe UI',Helvetica,Arial,sans-serif;color:#FFFEF8;background:#4B5B1D;border:0;border-radius:999px;padding:10px 20px;cursor:pointer;">Imprimir / Guardar PDF</button>
                </div>
                """ : "";

        return """
                <!DOCTYPE html>
                <html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Boleta %s · Kampu</title>
                <style>
                  @media print { .no-print { display:none !important; } body { background:#FFFFFF !important; } }
                  @media only screen and (max-width:620px) { .col { display:block !important; width:100%% !important; padding:0 0 12px 0 !important; } .px { padding-left:20px !important; padding-right:20px !important; } }
                </style></head>
                <body style="margin:0;padding:0;background:#F2EDD6;font-family:'Segoe UI',Helvetica,Arial,sans-serif;color:#2C3610;">
                <div style="display:none;max-height:0;overflow:hidden;">Reserva %s · %s · Saldo %s</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#F2EDD6;padding:28px 12px;">
                <tr><td align="center">
                %s
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:640px;background:#FFFEF8;border:1px solid #E5DEC2;border-radius:22px;overflow:hidden;">
                  <tr><td style="height:6px;background:#4B5B1D;font-size:0;line-height:0;">&nbsp;</td></tr>

                  <!-- Encabezado: logo y número de boleta -->
                  <tr><td class="px" style="padding:28px 36px 22px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr>
                      <td class="col" valign="middle"><img src="%s" width="202" height="44" alt="Kampu" style="display:block;border:0;height:44px;width:202px;"></td>
                      <td class="col" valign="middle" align="right" style="text-align:right;">
                        <div style="font-size:11px;font-weight:800;letter-spacing:1.6px;text-transform:uppercase;color:#6B7E2B;">Boleta informativa</div>
                        <div style="margin-top:4px;font-size:26px;font-weight:800;color:#2C3610;font-family:Consolas,Menlo,monospace;letter-spacing:0.5px;">%s</div>
                        <div style="margin-top:2px;font-size:12px;color:#A49D7B;">Emitida el %s</div>
                      </td>
                    </tr></table>
                  </td></tr>

                  <tr><td class="px" style="padding:0 36px;"><div style="border-top:1px solid #E5DEC2;"></div></td></tr>

                  <!-- Saludo y estados -->
                  <tr><td class="px" style="padding:24px 36px 6px;">
                    <div style="font-size:20px;font-weight:800;color:#2C3610;">Hola, %s</div>
                    <p style="margin:8px 0 14px;font-size:14px;line-height:1.6;color:#5B5741;">Gracias por elegirnos. Aquí tienes el detalle de tu reserva; guárdalo para el día del viaje.</p>
                    %s %s
                  </td></tr>

                  <!-- Cliente y reserva -->
                  <tr><td class="px" style="padding:18px 36px 6px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr>
                      <td class="col" width="50%%" valign="top" style="padding-right:8px;">
                        <div style="border:1px solid #E5DEC2;border-radius:14px;padding:16px 18px;">
                          %s
                          %s
                          %s
                          %s
                        </div>
                      </td>
                      <td class="col" width="50%%" valign="top" style="padding-left:8px;">
                        <div style="border:1px solid #E5DEC2;border-radius:14px;padding:16px 18px;">
                          %s
                          %s
                          %s
                          %s
                        </div>
                      </td>
                    </tr></table>
                  </td></tr>

                  <!-- Tour -->
                  <tr><td class="px" style="padding:12px 36px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#F4F6E4;border-radius:16px;">
                      <tr><td style="padding:20px 22px 6px;">
                        <div style="font-size:11px;font-weight:800;letter-spacing:1.4px;text-transform:uppercase;color:#6B7E2B;">Tu tour</div>
                        <div style="margin-top:4px;font-size:21px;font-weight:800;color:#2C3610;">%s</div>
                        <div style="margin-top:2px;font-size:13px;color:#5B5741;">%s días / %s noches · Guía: %s</div>
                      </td></tr>
                      <tr><td style="padding:12px 22px 20px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr>
                          <td class="col" width="50%%" valign="top" style="padding-right:6px;">
                            <div style="background:#FFFEF8;border-radius:12px;padding:12px 14px;">
                              <div style="font-size:11px;font-weight:800;letter-spacing:1px;text-transform:uppercase;color:#78735A;">Salida</div>
                              <div style="margin-top:4px;font-size:15px;font-weight:800;color:#2C3610;">%s</div>
                              <div style="font-size:12px;color:#78735A;">%s</div>
                            </div>
                          </td>
                          <td class="col" width="50%%" valign="top" style="padding-left:6px;">
                            <div style="background:#FFFEF8;border-radius:12px;padding:12px 14px;">
                              <div style="font-size:11px;font-weight:800;letter-spacing:1px;text-transform:uppercase;color:#78735A;">Retorno</div>
                              <div style="margin-top:4px;font-size:15px;font-weight:800;color:#2C3610;">%s</div>
                              <div style="font-size:12px;color:#78735A;">%s</div>
                            </div>
                          </td>
                        </tr></table>
                      </td></tr>
                    </table>
                  </td></tr>

                  <!-- Pasajeros -->
                  <tr><td class="px" style="padding:16px 36px 4px;">
                    %s
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="font-size:13px;">
                      <tr>%s%s%s%s</tr>
                      %s
                    </table>
                  </td></tr>

                  %s

                  <!-- Resumen de pago -->
                  <tr><td class="px" style="padding:18px 36px 8px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #E5DEC2;border-radius:16px;">
                      <tr><td style="padding:18px 20px 6px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="font-size:14px;">
                          <tr><td style="padding:3px 0;color:#78735A;">%d viajero(s) × %s</td><td align="right" style="padding:3px 0;color:#2C3610;">%s</td></tr>
                          <tr><td style="padding:3px 0;color:#78735A;">Pagado</td><td align="right" style="padding:3px 0;color:#36521E;font-weight:700;">%s</td></tr>
                        </table>
                      </td></tr>
                      <tr><td style="padding:8px 20px 4px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr>%s</tr></table>
                        <div style="margin-top:6px;font-size:12px;color:#A49D7B;">%d%% pagado</div>
                      </td></tr>
                      <tr><td style="padding:10px 20px 18px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr>
                          <td style="font-size:15px;font-weight:800;color:#2C3610;">%s</td>
                          <td align="right" style="font-size:26px;font-weight:900;color:%s;white-space:nowrap;">%s</td>
                        </tr></table>
                      </td></tr>
                    </table>
                  </td></tr>

                  <!-- Recordatorio -->
                  <tr><td class="px" style="padding:12px 36px 8px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#FAF9E3;border-radius:12px;">
                      <tr><td style="padding:12px 16px;font-size:13px;line-height:1.55;color:#666120;">
                        <strong>Importante:</strong> el día de la salida presenta esta boleta y el documento de identidad de cada pasajero.
                      </td></tr>
                    </table>
                  </td></tr>

                  <!-- Pie -->
                  <tr><td class="px" style="padding:22px 36px 28px;">
                    <div style="border-top:1px solid #E5DEC2;padding-top:16px;font-size:12px;line-height:1.7;color:#78735A;">
                      <strong style="color:#2C3610;">Kampu Tour Manager</strong> · %s · %s · %s<br>
                      Reserva registrada el %s. Documento informativo, no es un comprobante de pago tributario.
                    </div>
                  </td></tr>
                </table>
                <p style="margin:16px 0 0;font-size:11px;color:#A49D7B;">Recibiste este correo porque tienes una reserva con Kampu.</p>
                </td></tr></table>
                </body></html>
                """.formatted(
                codigo(r),
                codigo(r), esc(d.paquete.getNombre()), soles(saldo),
                barraNavegador,
                srcLogo,
                codigo(r), LocalDateTime.now().format(FECHA_HORA),
                esc(d.nombre),
                pillReserva, pillPago,
                titulo("Cliente"),
                dato("Nombre", esc(d.nombre)),
                dato("Documento", esc(d.documento)),
                dato("Contacto", esc(d.email) + "<br>" + esc(d.telefono)),
                titulo("Reserva"),
                dato("Código", codigo(r)),
                dato("Fecha de reserva", r.getFechaReserva() == null ? "—" : r.getFechaReserva().format(FECHA_HORA)),
                dato("Viajeros", String.valueOf(d.viajeros)),
                esc(d.paquete.getNombre()),
                d.paquete.getDias(), d.paquete.getNoches(), d.guia == null ? "por asignar" : esc(d.guia.getNombreCompleto()),
                fechaLarga(d.salida.getFechaInicio()), diaSemana(d.salida.getFechaInicio()),
                fechaLarga(d.salida.getFechaFin()), diaSemana(d.salida.getFechaFin()),
                titulo("Pasajeros (" + d.pasajeros.size() + ")"),
                encabezado("#", ""), encabezado("Nombre", ""), encabezado("Documento", ""), encabezado("Nacionalidad", "text-align:right;"),
                filasPasajeros,
                d.pagos.isEmpty() ? "" : bloquePagos(filasPagos.toString()),
                d.viajeros, soles(d.detalle.getPrecioUnitario()), soles(r.getMontoTotal()),
                soles(pagado),
                barraAvance, avance,
                pagadoTotal ? "Pagado por completo" : "Saldo pendiente",
                pagadoTotal ? "#36521E" : "#2C3610",
                pagadoTotal ? soles(r.getMontoTotal()) : soles(saldo),
                AGENCIA_EMAIL, AGENCIA_TELEFONO, AGENCIA_CIUDAD,
                r.getFechaReserva() == null ? "—" : r.getFechaReserva().format(FECHA_HORA));
    }

    private String bloquePagos(String filas) {
        return """
                <tr><td class="px" style="padding:18px 36px 4px;">
                  %s
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="font-size:13px;">
                    <tr>%s%s%s%s</tr>
                    %s
                  </table>
                </td></tr>
                """.formatted(titulo("Pagos recibidos"),
                encabezado("Fecha", ""), encabezado("Método", ""), encabezado("Código", ""), encabezado("Monto", "text-align:right;"),
                filas);
    }

    private String titulo(String texto) {
        return "<div style=\"font-size:11px;font-weight:800;letter-spacing:1.4px;text-transform:uppercase;color:#78735A;padding-bottom:8px;\">"
                + texto + "</div>";
    }

    private String dato(String etiqueta, String valor) {
        return "<div style=\"margin-top:6px;\"><div style=\"font-size:11px;color:#A49D7B;\">" + etiqueta + "</div>"
                + "<div style=\"font-size:13px;font-weight:700;color:#2C3610;line-height:1.5;word-break:break-word;\">" + valor + "</div></div>";
    }

    private String pill(String texto, String fondo, String color) {
        return "<span style=\"display:inline-block;margin:0 6px 6px 0;padding:5px 12px;border-radius:999px;background:" + fondo
                + ";color:" + color + ";font-size:12px;font-weight:800;\">" + texto + "</span>";
    }

    private String encabezado(String texto, String estilo) {
        return "<td style=\"padding:8px 0;border-bottom:2px solid #E5DEC2;font-size:11px;font-weight:800;letter-spacing:0.8px;"
                + "text-transform:uppercase;color:#A49D7B;" + estilo + "\">" + texto + "</td>";
    }

    private String celda(String contenido, String estilo) {
        return "<td style=\"padding:9px 8px 9px 0;border-bottom:1px solid #F2EDD6;" + estilo + "\">" + contenido + "</td>";
    }
}
