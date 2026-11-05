package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaDTO;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaRequestDTO;
import pe.edu.upeu.lp2_gp2.entity.Chofer;
import pe.edu.upeu.lp2_gp2.entity.Reserva;
import pe.edu.upeu.lp2_gp2.entity.SalidaProgramada;
import pe.edu.upeu.lp2_gp2.entity.Transporte;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.*;

import java.util.List;

@Service
public class SalidaProgramadaServiceImpl implements ISalidaProgramadaService {
    private final SalidaProgramadaRepository salidaRepository;
    private final PaqueteTourRepository paqueteRepository;
    private final GuiaRepository guiaRepository;
    private final TransporteRepository transporteRepository;
    private final ChoferRepository choferRepository;
    private final ReservaRepository reservaRepository;

    public SalidaProgramadaServiceImpl(SalidaProgramadaRepository salidaRepository, PaqueteTourRepository paqueteRepository,
                                       GuiaRepository guiaRepository, TransporteRepository transporteRepository,
                                       ChoferRepository choferRepository, ReservaRepository reservaRepository) {
        this.salidaRepository = salidaRepository;
        this.paqueteRepository = paqueteRepository;
        this.guiaRepository = guiaRepository;
        this.transporteRepository = transporteRepository;
        this.choferRepository = choferRepository;
        this.reservaRepository = reservaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalidaProgramadaDTO> listarTodo() {
        return salidaRepository.findAllByOrderBySalidaIdAsc().stream().map(SalidaProgramadaDTO::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalidaProgramadaDTO buscarPorId(Long id) {
        return SalidaProgramadaDTO.desde(salidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada con Id: " + id)));
    }

    @Override
    @Transactional
    public SalidaProgramadaDTO crear(SalidaProgramadaRequestDTO dto) {
        validar(dto, null);
        SalidaProgramada s = new SalidaProgramada();
        copiar(dto, s);
        s.setCuposDisponibles(dto.cuposMaximos());
        return SalidaProgramadaDTO.desde(salidaRepository.save(s));
    }

    @Override
    @Transactional
    public SalidaProgramadaDTO actualizar(Long id, SalidaProgramadaRequestDTO dto) {
        SalidaProgramada s = salidaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada con Id: " + id));
        validar(dto, id);
        int ocupados = viajerosActivos(id);
        if (dto.cuposMaximos() < ocupados) {
            throw new BusinessException("Ya hay " + ocupados + " viajeros reservados; los cupos no pueden ser menores.");
        }
        if ("CANCELADA".equals(dto.estado()) && ocupados > 0) {
            throw new BusinessException("No puedes cancelar una salida con reservas activas. Anúlalas primero.");
        }
        copiar(dto, s);
        s.setCuposDisponibles(dto.cuposMaximos() - ocupados);
        return SalidaProgramadaDTO.desde(salidaRepository.save(s));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        SalidaProgramada s = salidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada con Id: " + id));
        if (reservaRepository.existsBySalidaId(id)) {
            throw new BusinessException("La salida tiene reservas asociadas. Puedes marcarla como CANCELADA.");
        }
        salidaRepository.delete(s);
    }

    private void validar(SalidaProgramadaRequestDTO d, Long id) {
        if (!paqueteRepository.existsById(d.paqueteId())) throw new BusinessException("Selecciona un paquete.");
        if (d.fechaFin().isBefore(d.fechaInicio())) {
            throw new BusinessException("La fecha fin no puede ser anterior a la fecha inicio.");
        }
        if (d.guiaId() != null && !guiaRepository.existsById(d.guiaId())) throw new BusinessException("El guía seleccionado no existe.");
        if (d.transporteId() != null) {
            Transporte t = transporteRepository.findById(d.transporteId())
                    .orElseThrow(() -> new BusinessException("El transporte seleccionado no existe."));
            if (d.cuposMaximos() > t.getCapacidad()) {
                throw new BusinessException("Los cupos superan la capacidad del transporte (" + t.getCapacidad() + ").");
            }
        }
        if (d.choferId() != null) {
            if (d.transporteId() == null) throw new BusinessException("Asigna primero el vehículo que manejará el chofer.");
            Chofer c = choferRepository.findById(d.choferId())
                    .orElseThrow(() -> new BusinessException("El chofer seleccionado no existe."));
            if (!"ACTIVO".equals(c.getEstado())) throw new BusinessException("El chofer " + c.getNombreCompleto() + " está inactivo.");
        }
        boolean activa = "PROGRAMADA".equals(d.estado()) || "CONFIRMADA".equals(d.estado());
        List<String> estadosActivos = List.of("PROGRAMADA", "CONFIRMADA");
        if (activa && d.guiaId() != null) {
            List<SalidaProgramada> cruces = salidaRepository
                    .findByGuiaIdAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                            d.guiaId(), estadosActivos, d.fechaFin(), d.fechaInicio());
            if (hayOtraSalida(cruces, id)) throw new BusinessException("El guía ya está asignado a otra salida en esas fechas.");
        }
        if (activa && d.transporteId() != null) {
            List<SalidaProgramada> cruces = salidaRepository
                    .findByTransporteIdAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                            d.transporteId(), estadosActivos, d.fechaFin(), d.fechaInicio());
            if (hayOtraSalida(cruces, id)) throw new BusinessException("El transporte ya está asignado a otra salida en esas fechas.");
        }
        if (activa && d.choferId() != null) {
            List<SalidaProgramada> cruces = salidaRepository
                    .findByChoferIdAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                            d.choferId(), estadosActivos, d.fechaFin(), d.fechaInicio());
            if (hayOtraSalida(cruces, id)) throw new BusinessException("El chofer ya está asignado a otra salida en esas fechas.");
        }
    }

    private boolean hayOtraSalida(List<SalidaProgramada> salidas, Long idActual) {
        for (SalidaProgramada s : salidas) {
            if (idActual == null || !s.getSalidaId().equals(idActual)) return true;
        }
        return false;
    }

    private int viajerosActivos(Long salidaId) {
        int total = 0;
        for (Reserva r : reservaRepository.findBySalidaIdAndEstadoReservaNot(salidaId, "ANULADA")) {
            total = total + r.getNumeroViajeros();
        }
        return total;
    }

    private void copiar(SalidaProgramadaRequestDTO d, SalidaProgramada s) {
        s.setPaqueteId(d.paqueteId());
        s.setFechaInicio(d.fechaInicio());
        s.setFechaFin(d.fechaFin());
        s.setCuposMaximos(d.cuposMaximos());
        s.setGuiaId(d.guiaId());
        s.setTransporteId(d.transporteId());
        s.setChoferId(d.choferId());
        s.setEstado(d.estado());
    }
}
