package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.ManifiestoDTO;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaDTO;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaRequestDTO;
import pe.edu.upeu.lp2_gp2.entity.*;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.*;
import pe.edu.upeu.lp2_gp2.seguridad.SesionActual;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Salidas programadas: reglas del tour manager.
 *  - fecha_fin = inicio + días del paquete - 1; cupos_disponibles = máximos - vendidos.
 *  - Estados: PROGRAMADA -> CONFIRMADA -> COMPLETADA, con CANCELADA desde las dos primeras.
 *    COMPLETADA y CANCELADA son finales: la salida ya no se modifica.
 *  - Confirmar exige guía, al menos un pasajero y transporte suficiente en cada tramo.
 *  - Terminar exige que el viaje haya empezado, sin reservas pendientes ni traslados abiertos.
 *  - Cancelar exige que no queden reservas activas y cancela los traslados que no han salido.
 *  - El guía es PERSONAL con rol GUIA o AMBOS, sin cruce de fechas con otra salida.
 */
@Service
public class SalidaProgramadaServiceImpl implements ISalidaProgramadaService {
    private static final String PROGRAMADA = "PROGRAMADA";
    private static final String CONFIRMADA = "CONFIRMADA";
    private static final String COMPLETADA = "COMPLETADA";
    private static final String CANCELADA = "CANCELADA";

    // Transiciones permitidas desde cada estado
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            PROGRAMADA, Set.of(CONFIRMADA, CANCELADA),
            CONFIRMADA, Set.of(PROGRAMADA, COMPLETADA, CANCELADA),   // PROGRAMADA = reabrir
            COMPLETADA, Set.of(),
            CANCELADA, Set.of());

    private final SalidaProgramadaRepository salidaRepository;
    private final PaqueteTourRepository paqueteRepository;
    private final PersonalRepository personalRepository;
    private final DetalleReservaRepository detalleRepository;
    private final ReservaRepository reservaRepository;
    private final PasajeroReservaRepository pasajeroRepository;
    private final ClienteRepository clienteRepository;
    private final OperacionTransporteRepository operacionRepository;
    private final HistorialOperacionRepository historialRepository;
    private final CapacidadTransporte capacidad;

    public SalidaProgramadaServiceImpl(SalidaProgramadaRepository salidaRepository, PaqueteTourRepository paqueteRepository,
                                       PersonalRepository personalRepository, DetalleReservaRepository detalleRepository,
                                       ReservaRepository reservaRepository, PasajeroReservaRepository pasajeroRepository,
                                       ClienteRepository clienteRepository, OperacionTransporteRepository operacionRepository,
                                       HistorialOperacionRepository historialRepository, CapacidadTransporte capacidad) {
        this.salidaRepository = salidaRepository;
        this.paqueteRepository = paqueteRepository;
        this.personalRepository = personalRepository;
        this.detalleRepository = detalleRepository;
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.clienteRepository = clienteRepository;
        this.operacionRepository = operacionRepository;
        this.historialRepository = historialRepository;
        this.capacidad = capacidad;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalidaProgramadaDTO> listarTodo() {
        Map<Long, Integer> operaciones = new HashMap<>();
        for (OperacionTransporte o : operacionRepository.findAll()) {
            if (o.getSalidaId() != null && !"CANCELADO".equals(o.getEstado())) operaciones.merge(o.getSalidaId(), 1, Integer::sum);
        }
        return salidaRepository.findAllByOrderBySalidaIdAsc().stream()
                .map(s -> SalidaProgramadaDTO.desde(s, operaciones.get(s.getSalidaId()), faltante(s))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalidaProgramadaDTO buscarPorId(Long id) {
        return dto(obtener(id));
    }

    @Override
    @Transactional
    public SalidaProgramadaDTO crear(SalidaProgramadaRequestDTO dto) {
        if (!PROGRAMADA.equals(dto.estado())) {
            throw new BusinessException("Una salida nueva empieza como PROGRAMADA; se confirma cuando tiene guía, pasajeros y transporte.");
        }
        PaqueteTour paquete = validar(dto, null);
        if (!"ACTIVO".equals(paquete.getEstado())) {
            throw new BusinessException("El paquete " + paquete.getNombre() + " está inactivo: actívalo antes de programar salidas.");
        }
        if (dto.fechaInicio().isBefore(LocalDate.now())) throw new BusinessException("La fecha de inicio no puede ser anterior a hoy.");
        SalidaProgramada s = new SalidaProgramada();
        copiar(dto, s, paquete);
        s.setEstado(PROGRAMADA);
        s.setCuposDisponibles(dto.cuposMaximos());   // salida nueva = sin reservas
        return dto(salidaRepository.saveAndFlush(s));
    }

    @Override
    @Transactional
    public SalidaProgramadaDTO actualizar(Long id, SalidaProgramadaRequestDTO dto) {
        SalidaProgramada s = bloquear(id);
        if (TRANSICIONES.get(s.getEstado()).isEmpty()) {
            throw new BusinessException("La salida está " + s.getEstado() + " y ya no se puede modificar.");
        }
        PaqueteTour paquete = validar(dto, id);
        int vendidos = vendidos(s);
        if (dto.cuposMaximos() < vendidos) {
            throw new BusinessException("Ya hay " + vendidos + " viajeros reservados; los cupos no pueden ser menores.");
        }
        if (vendidos > 0 && !s.getPaqueteId().equals(dto.paqueteId())) {
            throw new BusinessException("La salida ya tiene reservas: no se puede cambiar el paquete que compraron los clientes.");
        }
        if (!s.getFechaInicio().equals(dto.fechaInicio()) && dto.fechaInicio().isBefore(LocalDate.now())) {
            throw new BusinessException("No se puede reprogramar la salida a una fecha anterior a hoy.");
        }
        String nuevoEstado = dto.estado();
        String estadoAnterior = s.getEstado();
        copiar(dto, s, paquete);
        s.setEstado(estadoAnterior);
        s.setCuposDisponibles(dto.cuposMaximos() - vendidos);
        salidaRepository.saveAndFlush(s);
        if (!estadoAnterior.equals(nuevoEstado)) cambiarEstado(s, nuevoEstado);
        return dto(s);
    }

    @Override
    @Transactional
    public SalidaProgramadaDTO actualizarEjecucion(Long id, String estado) {
        SalidaProgramada s = bloquear(id);
        if (s.getEstado().equals(estado)) throw new BusinessException("La salida ya está " + estado + ".");
        cambiarEstado(s, estado);
        return dto(s);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        SalidaProgramada s = obtener(id);
        if (detalleRepository.existsBySalidaId(id)) {
            throw new BusinessException("La salida tiene reservas asociadas. Puedes marcarla como CANCELADA.");
        }
        if (operacionRepository.existsBySalidaId(id)) {
            throw new BusinessException("La salida tiene operaciones de transporte. Cancélalas o márcala como CANCELADA.");
        }
        salidaRepository.delete(s);
    }

    @Override
    @Transactional(readOnly = true)
    public ManifiestoDTO manifiesto(Long id, boolean conMontos) {
        SalidaProgramada s = obtener(id);
        PaqueteTour paquete = paqueteRepository.findById(s.getPaqueteId()).orElseThrow();
        Personal guia = s.getGuiaId() == null ? null : personalRepository.findById(s.getGuiaId()).orElse(null);
        LocalDate hoy = LocalDate.now();
        List<ManifiestoDTO.Pasajero> pasajeros = new ArrayList<>();
        int confirmados = 0;
        int pendientes = 0;
        for (DetalleReserva d : detalleRepository.findBySalidaId(id)) {
            Reserva r = reservaRepository.findById(d.getReservaId()).orElse(null);
            if (r == null || "ANULADA".equals(r.getEstadoReserva())) continue;
            if ("PENDIENTE".equals(r.getEstadoReserva())) pendientes += d.getCantidad();
            else confirmados += d.getCantidad();
            Cliente titular = clienteRepository.findById(r.getClienteId()).orElse(null);
            BigDecimal saldo = r.getMontoTotal().subtract(r.getMontoPagado() == null ? BigDecimal.ZERO : r.getMontoPagado()).max(BigDecimal.ZERO);
            for (PasajeroReserva p : pasajeroRepository.findByDetalleIdInOrderByPasajeroIdAsc(List.of(d.getDetalleId()))) {
                Integer edad = p.getFechaNacimiento() == null ? null : Period.between(p.getFechaNacimiento(), hoy).getYears();
                pasajeros.add(new ManifiestoDTO.Pasajero(p.getPasajeroId(), p.getNombreCompleto(), p.getTipoDocumento(),
                        p.getNumeroDocumento(), p.getNacionalidad(), edad, r.getReservaId(), r.getEstadoReserva(),
                        titular == null ? null : titular.getNombreCompleto(), titular == null ? null : titular.getTelefono(),
                        r.getObservaciones(), conMontos ? r.getEstadoPago() : null, conMontos ? saldo : null));
            }
        }
        return new ManifiestoDTO(s.getSalidaId(), paquete.getNombre(), s.getFechaInicio(), s.getFechaFin(), s.getEstado(),
                guia == null ? null : guia.getNombreCompleto(), guia == null ? null : guia.getTelefono(),
                s.getCuposMaximos(), confirmados, pendientes, pasajeros);
    }

    // ---------- Máquina de estados ----------

    private void cambiarEstado(SalidaProgramada s, String nuevo) {
        String actual = s.getEstado();
        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevo)) {
            throw new BusinessException("Una salida " + actual + " no puede pasar a " + nuevo + ".");
        }
        switch (nuevo) {
            case CONFIRMADA -> exigirListaParaConfirmar(s);
            case COMPLETADA -> exigirListaParaTerminar(s);
            case CANCELADA -> exigirCancelable(s);
            default -> { }   // PROGRAMADA: se reabre para seguir ajustándola
        }
        s.setEstado(nuevo);
        salidaRepository.saveAndFlush(s);
        if (COMPLETADA.equals(nuevo)) completarReservas(s.getSalidaId());
        if (CANCELADA.equals(nuevo)) cancelarOperaciones(s.getSalidaId());
    }

    private void exigirListaParaConfirmar(SalidaProgramada s) {
        int pasajeros = vendidos(s);
        if (s.getGuiaId() == null) throw new BusinessException("Asigna un guía antes de confirmar la salida.");
        if (pasajeros == 0) throw new BusinessException("La salida aún no tiene pasajeros: no hay nada que confirmar.");
        String falta = capacidad.faltante(s.getSalidaId(), pasajeros, null);
        if (falta != null) throw new BusinessException("No se puede confirmar la salida: falta transporte en el " + falta + ".");
    }

    private void exigirListaParaTerminar(SalidaProgramada s) {
        if (s.getFechaInicio().isAfter(LocalDate.now())) {
            throw new BusinessException("La salida aún no empieza (" + s.getFechaInicio() + "): no puede marcarse como terminada.");
        }
        int pendientes = 0;
        for (DetalleReserva d : detalleRepository.findBySalidaId(s.getSalidaId())) {
            Reserva r = reservaRepository.findById(d.getReservaId()).orElse(null);
            if (r != null && "PENDIENTE".equals(r.getEstadoReserva())) pendientes++;
        }
        if (pendientes > 0) {
            throw new BusinessException("Hay " + pendientes + " reserva(s) pendiente(s) en esta salida: confírmalas o anúlalas antes de terminarla.");
        }
        long abiertas = operacionRepository.findBySalidaIdOrderByHoraSalidaAscOperacionIdAsc(s.getSalidaId()).stream()
                .filter(o -> !EstadoOperacion.de(o.getEstado()).esFinal()).count();
        if (abiertas > 0) {
            throw new BusinessException("Hay " + abiertas + " traslado(s) sin cerrar: finalízalos o cancélalos en Tráfico antes de terminar la salida.");
        }
    }

    private void exigirCancelable(SalidaProgramada s) {
        if (vendidos(s) > 0) throw new BusinessException("No puedes cancelar una salida con reservas activas. Anúlalas primero.");
        for (OperacionTransporte o : operacionRepository.findBySalidaIdOrderByHoraSalidaAscOperacionIdAsc(s.getSalidaId())) {
            EstadoOperacion estado = EstadoOperacion.de(o.getEstado());
            if (!estado.esFinal() && !estado.puedePasarA(EstadoOperacion.CANCELADO)) {
                throw new BusinessException("El traslado " + o.getCodigoOperacion() + " ya está en ruta: finalízalo antes de cancelar la salida.");
            }
        }
    }

    // Al terminar el viaje, las reservas confirmadas quedan COMPLETADAS (queda en AUDITORIA)
    private void completarReservas(Long salidaId) {
        for (DetalleReserva d : detalleRepository.findBySalidaId(salidaId)) {
            reservaRepository.findById(d.getReservaId()).ifPresent(r -> {
                if (CONFIRMADA.equals(r.getEstadoReserva())) {
                    r.setEstadoReserva(COMPLETADA);
                    reservaRepository.save(r);
                }
            });
        }
        reservaRepository.flush();
    }

    // Una salida cancelada no deja traslados reservando vehículos y conductores
    private void cancelarOperaciones(Long salidaId) {
        for (OperacionTransporte o : operacionRepository.findBySalidaIdOrderByHoraSalidaAscOperacionIdAsc(salidaId)) {
            if (EstadoOperacion.de(o.getEstado()).esFinal()) continue;
            o.setEstado(EstadoOperacion.CANCELADO.name());
            operacionRepository.save(o);
            HistorialOperacion h = new HistorialOperacion();
            h.setOperacionId(o.getOperacionId());
            h.setEstadoNuevo(o.getEstado());
            h.setFechaCambio(LocalDateTime.now());
            h.setUsuario(SesionActual.usuario());
            h.setObservacion("Salida cancelada");
            historialRepository.save(h);
        }
        operacionRepository.flush();
    }

    // ---------- Apoyo ----------

    private int vendidos(SalidaProgramada s) {
        return s.getCuposMaximos() - s.getCuposDisponibles();
    }

    private String faltante(SalidaProgramada s) {
        boolean activa = PROGRAMADA.equals(s.getEstado()) || CONFIRMADA.equals(s.getEstado());
        return activa ? capacidad.faltante(s.getSalidaId(), vendidos(s), null) : null;
    }

    private SalidaProgramadaDTO dto(SalidaProgramada s) {
        int operaciones = (int) operacionRepository.findBySalidaIdOrderByHoraSalidaAscOperacionIdAsc(s.getSalidaId()).stream()
                .filter(o -> !"CANCELADO".equals(o.getEstado())).count();
        return SalidaProgramadaDTO.desde(s, operaciones, faltante(s));
    }

    private SalidaProgramada obtener(Long id) {
        return salidaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada con Id: " + id));
    }

    private SalidaProgramada bloquear(Long id) {
        return salidaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada con Id: " + id));
    }

    private PaqueteTour validar(SalidaProgramadaRequestDTO d, Long id) {
        PaqueteTour paquete = paqueteRepository.findById(d.paqueteId()).orElseThrow(() -> new BusinessException("Selecciona un paquete."));
        if (d.guiaId() != null) {
            Personal g = personalRepository.findById(d.guiaId()).orElseThrow(() -> new BusinessException("El guía seleccionado no existe."));
            if ("CONDUCTOR".equals(g.getRol())) throw new BusinessException(g.getNombreCompleto() + " es conductor, no guía.");
            if ("INACTIVO".equals(g.getEstado())) throw new BusinessException("El guía " + g.getNombreCompleto() + " está inactivo.");
            boolean activa = PROGRAMADA.equals(d.estado()) || CONFIRMADA.equals(d.estado());
            if (activa) {
                LocalDate fin = d.fechaInicio().plusDays(paquete.getDias() - 1L);
                for (SalidaProgramada s : salidaRepository.findByGuiaIdAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                        d.guiaId(), List.of(PROGRAMADA, CONFIRMADA), fin, d.fechaInicio())) {
                    if (id == null || !s.getSalidaId().equals(id)) {
                        throw new BusinessException(g.getNombreCompleto() + " ya guía otra salida en esas fechas.");
                    }
                }
            }
        }
        return paquete;
    }

    private void copiar(SalidaProgramadaRequestDTO d, SalidaProgramada s, PaqueteTour paquete) {
        s.setPaqueteId(d.paqueteId());
        s.setFechaInicio(d.fechaInicio());
        s.setFechaFin(d.fechaInicio().plusDays(paquete.getDias() - 1L));
        s.setCuposMaximos(d.cuposMaximos());
        s.setGuiaId(d.guiaId());
        s.setEstado(d.estado());
    }
}
