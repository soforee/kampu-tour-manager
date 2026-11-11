package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaDTO;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaRequestDTO;
import pe.edu.upeu.lp2_gp2.entity.*;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Salidas programadas (v4: reglas en el backend).
 *  - fecha_fin = inicio + días del paquete - 1; cupos_disponibles = máximos - vendidos.
 *  - Solo se confirma si el transporte asignado alcanza para los pasajeros vendidos.
 *  - El guía es PERSONAL con rol GUIA o AMBOS, sin cruce de fechas con otra salida.
 */
@Service
public class SalidaProgramadaServiceImpl implements ISalidaProgramadaService {
    private final SalidaProgramadaRepository salidaRepository;
    private final PaqueteTourRepository paqueteRepository;
    private final PersonalRepository personalRepository;
    private final DetalleReservaRepository detalleRepository;
    private final ReservaRepository reservaRepository;
    private final OperacionTransporteRepository operacionRepository;
    private final CapacidadTransporte capacidad;

    public SalidaProgramadaServiceImpl(SalidaProgramadaRepository salidaRepository, PaqueteTourRepository paqueteRepository,
                                       PersonalRepository personalRepository, DetalleReservaRepository detalleRepository,
                                       ReservaRepository reservaRepository, OperacionTransporteRepository operacionRepository,
                                       CapacidadTransporte capacidad) {
        this.salidaRepository = salidaRepository;
        this.paqueteRepository = paqueteRepository;
        this.personalRepository = personalRepository;
        this.detalleRepository = detalleRepository;
        this.reservaRepository = reservaRepository;
        this.operacionRepository = operacionRepository;
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
        PaqueteTour paquete = validar(dto, null);
        SalidaProgramada s = new SalidaProgramada();
        copiar(dto, s, paquete);
        s.setCuposDisponibles(dto.cuposMaximos());   // salida nueva = sin reservas
        return dto(salidaRepository.saveAndFlush(s));
    }

    @Override
    @Transactional
    public SalidaProgramadaDTO actualizar(Long id, SalidaProgramadaRequestDTO dto) {
        SalidaProgramada s = salidaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada con Id: " + id));
        PaqueteTour paquete = validar(dto, id);
        int vendidos = s.getCuposMaximos() - s.getCuposDisponibles();
        if (dto.cuposMaximos() < vendidos) {
            throw new BusinessException("Ya hay " + vendidos + " viajeros reservados; los cupos no pueden ser menores.");
        }
        if ("CANCELADA".equals(dto.estado()) && !"CANCELADA".equals(s.getEstado()) && vendidos > 0) {
            throw new BusinessException("No puedes cancelar una salida con reservas activas. Anúlalas primero.");
        }
        String estadoAnterior = s.getEstado();
        copiar(dto, s, paquete);
        s.setCuposDisponibles(dto.cuposMaximos() - vendidos);
        if ("CONFIRMADA".equals(dto.estado()) && !"CONFIRMADA".equals(estadoAnterior)) exigirTransporte(s, vendidos);
        return dto(salidaRepository.saveAndFlush(s));
    }

    @Override
    @Transactional
    public SalidaProgramadaDTO actualizarEjecucion(Long id, String estado) {
        SalidaProgramada s = salidaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada con Id: " + id));
        if ("CANCELADA".equals(s.getEstado()) || "COMPLETADA".equals(s.getEstado())) {
            throw new BusinessException("La salida está " + s.getEstado() + ".");
        }
        if ("CONFIRMADA".equals(estado)) {
            exigirTransporte(s, s.getCuposMaximos() - s.getCuposDisponibles());
        } else if (s.getFechaInicio().isAfter(LocalDate.now())) {
            throw new BusinessException("La salida aún no empieza (" + s.getFechaInicio() + "): no puede marcarse como terminada.");
        }
        s.setEstado(estado);
        salidaRepository.saveAndFlush(s);
        // Al terminar el viaje, las reservas confirmadas quedan COMPLETADAS (queda en AUDITORIA)
        if ("COMPLETADA".equals(estado)) {
            for (DetalleReserva d : detalleRepository.findBySalidaId(id)) {
                reservaRepository.findById(d.getReservaId()).ifPresent(r -> {
                    if ("CONFIRMADA".equals(r.getEstadoReserva())) {
                        r.setEstadoReserva("COMPLETADA");
                        reservaRepository.save(r);
                    }
                });
            }
            reservaRepository.flush();
        }
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

    private void exigirTransporte(SalidaProgramada s, int pasajeros) {
        String falta = capacidad.faltante(s.getSalidaId(), pasajeros, null);
        if (falta != null) throw new BusinessException("No se puede confirmar la salida " + s.getSalidaId() + ": " + falta);
    }

    private String faltante(SalidaProgramada s) {
        boolean activa = "PROGRAMADA".equals(s.getEstado()) || "CONFIRMADA".equals(s.getEstado());
        return activa ? capacidad.faltante(s.getSalidaId(), s.getCuposMaximos() - s.getCuposDisponibles(), null) : null;
    }

    private SalidaProgramadaDTO dto(SalidaProgramada s) {
        int operaciones = (int) operacionRepository.findBySalidaIdOrderByHoraSalidaAscOperacionIdAsc(s.getSalidaId()).stream()
                .filter(o -> !"CANCELADO".equals(o.getEstado())).count();
        return SalidaProgramadaDTO.desde(s, operaciones, faltante(s));
    }

    private SalidaProgramada obtener(Long id) {
        return salidaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada con Id: " + id));
    }

    private PaqueteTour validar(SalidaProgramadaRequestDTO d, Long id) {
        PaqueteTour paquete = paqueteRepository.findById(d.paqueteId()).orElseThrow(() -> new BusinessException("Selecciona un paquete."));
        if (d.guiaId() != null) {
            Personal g = personalRepository.findById(d.guiaId()).orElseThrow(() -> new BusinessException("El guía seleccionado no existe."));
            if ("CONDUCTOR".equals(g.getRol())) throw new BusinessException(g.getNombreCompleto() + " es conductor, no guía.");
            if ("INACTIVO".equals(g.getEstado())) throw new BusinessException("El guía " + g.getNombreCompleto() + " está inactivo.");
            boolean activa = "PROGRAMADA".equals(d.estado()) || "CONFIRMADA".equals(d.estado());
            if (activa) {
                LocalDate fin = d.fechaInicio().plusDays(paquete.getDias() - 1L);
                for (SalidaProgramada s : salidaRepository.findByGuiaIdAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                        d.guiaId(), List.of("PROGRAMADA", "CONFIRMADA"), fin, d.fechaInicio())) {
                    if (id == null || !s.getSalidaId().equals(id)) throw new BusinessException("El guía ya está asignado a otra salida en esas fechas.");
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
