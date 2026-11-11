package pe.edu.upeu.lp2_gp2.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.*;
import pe.edu.upeu.lp2_gp2.entity.*;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Reservas v4: cabecera (RESERVAS) + detalle por salida (DETALLE_RESERVA) + pasajeros.
 * Cupos, precio y monto_total los calcula el backend con la salida bloqueada
 * (SELECT ... FOR UPDATE), así dos ventas simultáneas no sobrevenden.
 * Los cambios de estado quedan en AUDITORIA (TRG_AUDITORIA_ESTADO_PAQUETE).
 */
@Service
public class ReservaServiceImpl implements IReservaService {
    private final ReservaRepository reservaRepository;
    private final DetalleReservaRepository detalleRepository;
    private final PasajeroReservaRepository pasajeroRepository;
    private final SalidaProgramadaRepository salidaRepository;
    private final PaqueteTourRepository paqueteRepository;
    private final ClienteRepository clienteRepository;
    private final IClienteService clienteService;
    private final UsuarioSistemaRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final CapacidadTransporte capacidad;
    private final ApplicationEventPublisher eventos;

    public ReservaServiceImpl(ReservaRepository reservaRepository, DetalleReservaRepository detalleRepository,
                              PasajeroReservaRepository pasajeroRepository, SalidaProgramadaRepository salidaRepository,
                              PaqueteTourRepository paqueteRepository, ClienteRepository clienteRepository,
                              IClienteService clienteService, UsuarioSistemaRepository usuarioRepository,
                              PagoRepository pagoRepository, CapacidadTransporte capacidad, ApplicationEventPublisher eventos) {
        this.reservaRepository = reservaRepository;
        this.detalleRepository = detalleRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.salidaRepository = salidaRepository;
        this.paqueteRepository = paqueteRepository;
        this.clienteRepository = clienteRepository;
        this.clienteService = clienteService;
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
        this.capacidad = capacidad;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaDTO> listarTodo() {
        Map<Long, DetalleReserva> detalles = new HashMap<>();
        for (DetalleReserva d : detalleRepository.findAllByOrderByDetalleIdAsc()) detalles.putIfAbsent(d.getReservaId(), d);
        Map<Long, String> usuarios = new HashMap<>();
        usuarioRepository.findAll().forEach(u -> usuarios.put(u.getUsuarioId(), u.getUsername()));
        return reservaRepository.findAllByOrderByReservaIdAsc().stream()
                .map(r -> ReservaDTO.desde(r, detalles.get(r.getReservaId()),
                        r.getUsuarioAnulaId() == null ? null : usuarios.get(r.getUsuarioAnulaId()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaDTO buscarPorId(Long id) {
        return dto(obtener(id));
    }

    @Override
    @Transactional
    public ReservaDTO crear(ReservaRequestDTO dto) {
        for (PasajeroRequestDTO p : dto.pasajeros()) {
            if (!Reglas.documentoValido(p.tipoDocumento(), p.numeroDocumento())) throw new BusinessException(Reglas.MSG_DOCUMENTO);
            if (p.fechaNacimiento() != null && p.fechaNacimiento().isBefore(Reglas.FECHA_MINIMA)) {
                throw new BusinessException("La fecha de nacimiento no puede ser anterior a 1900.");
            }
        }
        if (dto.clienteId() != null && dto.contacto() != null) {
            throw new BusinessException("Elige un cliente registrado o ingresa los datos del cliente nuevo, no ambos.");
        }
        if (dto.clienteId() == null && dto.contacto() == null) throw new BusinessException("Selecciona un cliente o registra uno nuevo.");
        if (dto.clienteId() != null && !clienteRepository.existsById(dto.clienteId())) throw new BusinessException("El cliente seleccionado no existe.");

        // Bloquea la salida: cupos consistentes aunque vendan dos personas a la vez
        SalidaProgramada salida = salidaRepository.findByIdParaActualizar(dto.salidaId())
                .orElseThrow(() -> new BusinessException("Selecciona una salida."));
        if (!"PROGRAMADA".equals(salida.getEstado()) && !"CONFIRMADA".equals(salida.getEstado())) {
            throw new BusinessException("La salida está " + salida.getEstado() + " y no acepta reservas.");
        }
        LocalDate hoy = LocalDate.now();
        if (salida.getFechaInicio().isBefore(hoy)) throw new BusinessException("La salida ya inició.");
        int n = dto.pasajeros().size();
        if (n > salida.getCuposDisponibles()) {
            throw new BusinessException("Solo quedan " + salida.getCuposDisponibles() + " cupo(s) en esta salida.");
        }
        // Salida ya confirmada: solo se vende lo que el transporte asignado puede llevar
        if ("CONFIRMADA".equals(salida.getEstado())) {
            int vendidos = salida.getCuposMaximos() - salida.getCuposDisponibles();
            String falta = capacidad.faltante(salida.getSalidaId(), vendidos + n, null);
            if (falta != null) throw new BusinessException("Sin transporte suficiente para vender " + n + " cupo(s) más: " + falta);
        }
        Set<String> documentos = new HashSet<>();
        for (PasajeroRequestDTO p : dto.pasajeros()) {
            if (p.fechaNacimiento() != null && !p.fechaNacimiento().isBefore(hoy)) {
                throw new BusinessException("La fecha de nacimiento de " + p.nombreCompleto().trim() + " debe ser anterior a hoy.");
            }
            if (!documentos.add(p.tipoDocumento() + ":" + p.numeroDocumento().trim().toUpperCase())) {
                throw new BusinessException("El documento " + p.numeroDocumento().trim() + " está repetido en la reserva.");
            }
            if (pasajeroRepository.contarEnSalida(salida.getSalidaId(), p.tipoDocumento(), p.numeroDocumento().trim()) > 0) {
                throw new BusinessException("El pasajero " + p.nombreCompleto().trim() + " (" + p.tipoDocumento() + " "
                        + p.numeroDocumento().trim() + ") ya tiene una reserva activa en esta salida.");
            }
        }

        // Cliente nuevo (v4: toda reserva tiene cliente; podrá activar su cuenta con "Registrarse")
        Long clienteId = dto.clienteId();
        if (clienteId == null) {
            ContactoReservaDTO c = dto.contacto();
            clienteId = clienteService.crear(new ClienteRequestDTO(c.nombre(), c.tipoDocumento(), c.numeroDocumento(),
                    c.email(), c.telefono())).clienteId();
        }

        PaqueteTour paquete = paqueteRepository.findById(salida.getPaqueteId()).orElseThrow();
        BigDecimal total = paquete.getPrecioBase().multiply(BigDecimal.valueOf(n));
        Reserva r = new Reserva();
        r.setClienteId(clienteId);
        r.setFechaReserva(LocalDateTime.now());
        r.setMontoTotal(total);
        r.setMontoPagado(BigDecimal.ZERO);
        r.setEstadoReserva("PENDIENTE");
        r.setEstadoPago("PENDIENTE");
        r.setObservaciones(dto.observaciones() == null || dto.observaciones().isBlank() ? null : dto.observaciones().trim());
        Reserva guardada = reservaRepository.saveAndFlush(r);

        DetalleReserva d = new DetalleReserva();
        d.setReservaId(guardada.getReservaId());
        d.setSalidaId(salida.getSalidaId());
        d.setCantidad(n);
        d.setPrecioUnitario(paquete.getPrecioBase());   // precio congelado al momento de la venta
        d.setDescuento(BigDecimal.ZERO);
        DetalleReserva detalle = detalleRepository.saveAndFlush(d);

        for (PasajeroRequestDTO p : dto.pasajeros()) {
            PasajeroReserva pr = new PasajeroReserva();
            pr.setDetalleId(detalle.getDetalleId());
            pr.setNombreCompleto(p.nombreCompleto().trim());
            pr.setTipoDocumento(p.tipoDocumento());
            pr.setNumeroDocumento(p.numeroDocumento().trim());
            pr.setNacionalidad(p.nacionalidad() == null || p.nacionalidad().isBlank() ? null : p.nacionalidad().trim());
            pr.setFechaNacimiento(p.fechaNacimiento());
            pasajeroRepository.save(pr);
        }
        salida.setCuposDisponibles(salida.getCuposDisponibles() - n);
        salidaRepository.save(salida);
        pasajeroRepository.flush();
        return ReservaDTO.desde(guardada, detalle);
    }

    @Override
    @Transactional
    public ReservaDTO confirmar(Long id) {
        Reserva r = obtener(id);
        if (!"PENDIENTE".equals(r.getEstadoReserva())) throw new BusinessException("Solo se confirman reservas pendientes.");
        r.setEstadoReserva("CONFIRMADA");
        reservaRepository.saveAndFlush(r);
        eventos.publishEvent(new BoletaEvento(id, "reserva confirmada"));
        return dto(r);
    }

    @Override
    @Transactional
    public ReservaDTO completar(Long id) {
        Reserva r = obtener(id);
        if (!"CONFIRMADA".equals(r.getEstadoReserva())) throw new BusinessException("Solo se completan reservas confirmadas.");
        r.setEstadoReserva("COMPLETADA");
        reservaRepository.saveAndFlush(r);
        return dto(r);
    }

    @Override
    @Transactional
    public ReservaDTO anular(Long id, Long usuarioId, String formaReembolso) {
        Reserva r = reservaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con Id: " + id));
        if (!"PENDIENTE".equals(r.getEstadoReserva()) && !"CONFIRMADA".equals(r.getEstadoReserva())) {
            throw new BusinessException("Solo se anulan reservas pendientes o confirmadas.");
        }
        // Quien anula es el usuario de la sesión (el id que manda el navegador no se usa: podría falsearse)
        var sesion = pe.edu.upeu.lp2_gp2.seguridad.SesionActual.get();
        if (sesion != null && sesion.usuarioId() != null) usuarioId = sesion.usuarioId();
        if (usuarioId == null) throw new BusinessException("Indica el usuario que anula la reserva (inicia sesión de nuevo).");
        if (!usuarioRepository.existsById(usuarioId)) throw new BusinessException("El usuario que anula no existe.");
        boolean hayReembolso = r.getMontoPagado() != null && r.getMontoPagado().signum() > 0;
        String forma = formaReembolso == null || formaReembolso.isBlank() ? null : formaReembolso.trim();
        if (hayReembolso && forma == null) throw new BusinessException("Indica la forma de reembolso.");

        for (Pago p : pagoRepository.findByReservaIdOrderByPagoId(id)) {
            if ("APROBADO".equals(p.getEstadoTransaccion())) p.setEstadoTransaccion("REEMBOLSADO");
            else if ("PENDIENTE".equals(p.getEstadoTransaccion())) p.setEstadoTransaccion("RECHAZADO");
            else continue;
            pagoRepository.save(p);
        }
        pagoRepository.flush();
        // Devuelve los cupos a cada salida reservada
        for (DetalleReserva d : detalleRepository.findByReservaIdOrderByDetalleIdAsc(id)) {
            salidaRepository.findByIdParaActualizar(d.getSalidaId()).ifPresent(s -> {
                s.setCuposDisponibles(Math.min(s.getCuposMaximos(), s.getCuposDisponibles() + d.getCantidad()));
                salidaRepository.save(s);
            });
        }
        r.setEstadoReserva("ANULADA");
        r.setMontoPagado(BigDecimal.ZERO);
        r.setEstadoPago(hayReembolso ? "REEMBOLSADO" : "PENDIENTE");
        r.setFormaReembolso(hayReembolso ? forma : null);
        r.setUsuarioAnulaId(usuarioId);
        reservaRepository.saveAndFlush(r);
        return dto(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasajeroReservaDTO> listarPasajeros(Long reservaId) {
        Map<Long, Long> reservaDeDetalle = new HashMap<>();
        List<PasajeroReserva> lista;
        if (reservaId == null) {
            for (DetalleReserva d : detalleRepository.findAll()) reservaDeDetalle.put(d.getDetalleId(), d.getReservaId());
            lista = pasajeroRepository.findAllByOrderByPasajeroIdAsc();
        } else {
            for (DetalleReserva d : detalleRepository.findByReservaIdOrderByDetalleIdAsc(reservaId)) {
                reservaDeDetalle.put(d.getDetalleId(), d.getReservaId());
            }
            lista = reservaDeDetalle.isEmpty() ? List.of()
                    : pasajeroRepository.findByDetalleIdInOrderByPasajeroIdAsc(reservaDeDetalle.keySet());
        }
        return lista.stream().map(p -> PasajeroReservaDTO.desde(p, reservaDeDetalle.get(p.getDetalleId()))).toList();
    }

    private ReservaDTO dto(Reserva r) {
        List<DetalleReserva> detalles = detalleRepository.findByReservaIdOrderByDetalleIdAsc(r.getReservaId());
        String anula = r.getUsuarioAnulaId() == null ? null
                : usuarioRepository.findById(r.getUsuarioAnulaId()).map(u -> u.getUsername()).orElse(null);
        return ReservaDTO.desde(r, detalles.isEmpty() ? null : detalles.get(0), anula);
    }

    private Reserva obtener(Long id) {
        return reservaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con Id: " + id));
    }
}
