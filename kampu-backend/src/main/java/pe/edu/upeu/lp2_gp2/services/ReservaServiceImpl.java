package pe.edu.upeu.lp2_gp2.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.PasajeroRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.PasajeroReservaDTO;
import pe.edu.upeu.lp2_gp2.dto.Reglas;
import pe.edu.upeu.lp2_gp2.dto.ReservaDTO;
import pe.edu.upeu.lp2_gp2.dto.ReservaRequestDTO;
import pe.edu.upeu.lp2_gp2.entity.*;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReservaServiceImpl implements IReservaService {
    private final ReservaRepository reservaRepository;
    private final PasajeroReservaRepository pasajeroRepository;
    private final SalidaProgramadaRepository salidaRepository;
    private final ClienteRepository clienteRepository;
    private final PaqueteTourRepository paqueteRepository;
    private final PagoRepository pagoRepository;
    private final ApplicationEventPublisher eventos;

    public ReservaServiceImpl(ReservaRepository reservaRepository, PasajeroReservaRepository pasajeroRepository,
                              SalidaProgramadaRepository salidaRepository, ClienteRepository clienteRepository,
                              PaqueteTourRepository paqueteRepository, PagoRepository pagoRepository,
                              ApplicationEventPublisher eventos) {
        this.reservaRepository = reservaRepository;
        this.pasajeroRepository = pasajeroRepository;
        this.salidaRepository = salidaRepository;
        this.clienteRepository = clienteRepository;
        this.paqueteRepository = paqueteRepository;
        this.pagoRepository = pagoRepository;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaDTO> listarTodo() {
        return reservaRepository.findAllByOrderByReservaIdAsc().stream().map(ReservaDTO::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaDTO buscarPorId(Long id) {
        return ReservaDTO.desde(obtener(id));
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
        validarCliente(dto);
        SalidaProgramada salida = salidaRepository.findByIdParaActualizar(dto.salidaId())
                .orElseThrow(() -> new BusinessException("Selecciona una salida."));
        if (!"PROGRAMADA".equals(salida.getEstado()) && !"CONFIRMADA".equals(salida.getEstado())) {
            throw new BusinessException("La salida no admite nuevas reservas.");
        }
        LocalDate hoy = LocalDate.now();
        if (salida.getFechaInicio().isBefore(hoy)) throw new BusinessException("La salida ya inició.");

        int n = dto.pasajeros().size();
        if (n > salida.getCuposDisponibles()) {
            throw new BusinessException("Solo quedan " + salida.getCuposDisponibles() + " cupos en esta salida.");
        }
        Set<String> documentos = new HashSet<>();
        for (PasajeroRequestDTO p : dto.pasajeros()) {
            if (p.fechaNacimiento() != null && !p.fechaNacimiento().isBefore(hoy)) {
                throw new BusinessException("La fecha de nacimiento de " + p.nombreCompleto().trim() + " debe ser anterior a hoy.");
            }
            if (!documentos.add(p.numeroDocumento().trim().toUpperCase())) {
                throw new BusinessException("El documento " + p.numeroDocumento().trim() + " está repetido en la reserva.");
            }
        }

        PaqueteTour paquete = paqueteRepository.findById(salida.getPaqueteId())
                .orElseThrow(() -> new BusinessException("El paquete de la salida no existe."));
        Reserva r = new Reserva();
        r.setClienteId(dto.clienteId());
        if (dto.clienteId() == null) {
            r.setContactoNombre(dto.contacto().nombre().trim());
            r.setContactoEmail(dto.contacto().email().trim().toLowerCase());
            r.setContactoTelefono(dto.contacto().telefono().trim());
        }
        r.setSalidaId(dto.salidaId());
        r.setFechaReserva(LocalDateTime.now());
        r.setNumeroViajeros(n);
        r.setMontoTotal(paquete.getPrecioBase().multiply(BigDecimal.valueOf(n)));
        r.setMontoPagado(BigDecimal.ZERO);
        r.setEstadoReserva("PENDIENTE");
        r.setEstadoPago("PENDIENTE");
        r.setObservaciones(dto.observaciones() == null || dto.observaciones().isBlank() ? null : dto.observaciones().trim());
        Reserva guardada = reservaRepository.save(r);

        for (PasajeroRequestDTO p : dto.pasajeros()) {
            PasajeroReserva pr = new PasajeroReserva();
            pr.setReservaId(guardada.getReservaId());
            pr.setNombreCompleto(p.nombreCompleto().trim());
            pr.setTipoDocumento(p.tipoDocumento());
            pr.setNumeroDocumento(p.numeroDocumento().trim());
            pr.setNacionalidad(p.nacionalidad() == null || p.nacionalidad().isBlank() ? null : p.nacionalidad().trim());
            pr.setFechaNacimiento(p.fechaNacimiento());
            pasajeroRepository.save(pr);
        }
        salida.setCuposDisponibles(salida.getCuposDisponibles() - n);
        salidaRepository.save(salida);
        return ReservaDTO.desde(guardada);
    }

    @Override
    @Transactional
    public ReservaDTO confirmar(Long id) {
        Reserva r = obtener(id);
        if (!"PENDIENTE".equals(r.getEstadoReserva())) throw new BusinessException("Solo se confirman reservas pendientes.");
        r.setEstadoReserva("CONFIRMADA");
        Reserva guardada = reservaRepository.save(r);
        eventos.publishEvent(new BoletaEvento(id, "reserva confirmada"));
        return ReservaDTO.desde(guardada);
    }

    @Override
    @Transactional
    public ReservaDTO completar(Long id) {
        Reserva r = obtener(id);
        if (!"CONFIRMADA".equals(r.getEstadoReserva())) throw new BusinessException("Solo se completan reservas confirmadas.");
        r.setEstadoReserva("COMPLETADA");
        return ReservaDTO.desde(reservaRepository.save(r));
    }

    @Override
    @Transactional
    public ReservaDTO anular(Long id, Long usuarioId, String formaReembolso) {
        Reserva r = reservaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con Id: " + id));
        if (!"PENDIENTE".equals(r.getEstadoReserva()) && !"CONFIRMADA".equals(r.getEstadoReserva())) {
            throw new BusinessException("Solo se anulan reservas pendientes o confirmadas.");
        }
        boolean hayReembolso = r.getMontoPagado() != null && r.getMontoPagado().signum() > 0;
        String forma = formaReembolso == null || formaReembolso.isBlank() ? null : formaReembolso.trim();
        if (hayReembolso && forma == null) throw new BusinessException("Indica la forma de reembolso.");

        for (Pago p : pagoRepository.findByReservaIdAndEstadoTransaccion(id, "APROBADO")) {
            p.setEstadoTransaccion("REEMBOLSADO");
            pagoRepository.save(p);
        }
        r.setEstadoReserva("ANULADA");
        r.setMontoPagado(BigDecimal.ZERO);
        r.setEstadoPago(hayReembolso ? "REEMBOLSADO" : "PENDIENTE");
        r.setFormaReembolso(hayReembolso ? forma : null);
        r.setUsuarioAnulaId(usuarioId);
        Reserva guardada = reservaRepository.save(r);

        Optional<SalidaProgramada> salida = salidaRepository.findByIdParaActualizar(r.getSalidaId());
        if (salida.isPresent()) {
            SalidaProgramada s = salida.get();
            s.setCuposDisponibles(Math.min(s.getCuposMaximos(), s.getCuposDisponibles() + r.getNumeroViajeros()));
            salidaRepository.save(s);
        }
        return ReservaDTO.desde(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasajeroReservaDTO> listarPasajeros(Long reservaId) {
        List<PasajeroReserva> lista = reservaId == null
                ? pasajeroRepository.findAllByOrderByPasajeroIdAsc()
                : pasajeroRepository.findByReservaIdOrderByPasajeroId(reservaId);
        return lista.stream().map(PasajeroReservaDTO::desde).toList();
    }

    // Exactamente uno: un cliente registrado o los datos de contacto del invitado.
    private void validarCliente(ReservaRequestDTO dto) {
        if (dto.clienteId() != null && dto.contacto() != null) {
            throw new BusinessException("Elige un cliente registrado o ingresa los datos del invitado, no ambos.");
        }
        if (dto.clienteId() == null && dto.contacto() == null) {
            throw new BusinessException("Selecciona un cliente o ingresa los datos del invitado.");
        }
        if (dto.clienteId() != null && !clienteRepository.existsById(dto.clienteId())) {
            throw new BusinessException("El cliente seleccionado no existe.");
        }
        if (dto.contacto() != null && !Reglas.telefonoValido(dto.contacto().telefono())) {
            throw new BusinessException(Reglas.MSG_TELEFONO);
        }
    }

    private Reserva obtener(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con Id: " + id));
    }
}
