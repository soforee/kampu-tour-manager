package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.auditoria.AuditoriaService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.*;
import pe.edu.upeu.lp2_gp2.entity.Pago;
import pe.edu.upeu.lp2_gp2.entity.VistaPaqueteCliente;
import pe.edu.upeu.lp2_gp2.exception.AccesoDenegadoException;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.repository.*;
import pe.edu.upeu.lp2_gp2.seguridad.SesionActual;
import pe.edu.upeu.lp2_gp2.seguridad.SesionUsuario;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Portal del cliente (solo lectura, salvo informar un pago):
 *  - VW_PAQUETES_CLIENTE: la vista ya devuelve solo los paquetes del cliente de la sesión
 *    (SesionOracleDataSource pasa el cliente a PKG_SEGURIDAD_ROLES en cada conexión).
 *  - PKG_GESTION_PAQUETES_CLIENTE: detalle_paquete (exige que sea suyo) e iniciar_pago.
 */
@Service
public class PortalClienteService {
    private final VistaPaqueteClienteRepository vistaRepository;
    private final PaqueteTourRepository paqueteRepository;
    private final SalidaProgramadaRepository salidaRepository;
    private final PersonalRepository personalRepository;
    private final PagoRepository pagoRepository;
    private final JdbcTemplate jdbc;
    private final AuditoriaService auditoria;

    public PortalClienteService(VistaPaqueteClienteRepository vistaRepository, PaqueteTourRepository paqueteRepository,
                                SalidaProgramadaRepository salidaRepository, PersonalRepository personalRepository,
                                PagoRepository pagoRepository, JdbcTemplate jdbc, AuditoriaService auditoria) {
        this.auditoria = auditoria;
        this.vistaRepository = vistaRepository;
        this.paqueteRepository = paqueteRepository;
        this.salidaRepository = salidaRepository;
        this.personalRepository = personalRepository;
        this.pagoRepository = pagoRepository;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<PortalPaqueteDTO> misPaquetes() {
        Long clienteId = clienteActual();
        return vistaRepository.findAllByOrderByFechaInicioDesc().stream()
                .filter(v -> clienteId.equals(v.getClienteId()))   // doble control además del filtro de la vista
                .map(PortalPaqueteDTO::desde).toList();
    }

    @Transactional(readOnly = true)
    public PortalDetalleDTO detalle(Long reservaId) {
        Long clienteId = clienteActual();
        // PKG_GESTION_PAQUETES_CLIENTE.detalle_paquete: ORA-20080 si la reserva no es del cliente
        List<PortalDetalleDTO.PortalPasajeroDTO> pasajeros = jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{call PKG_GESTION_PAQUETES_CLIENTE.detalle_paquete(?, ?, ?)}")) {
                cs.setLong(1, clienteId);
                cs.setLong(2, reservaId);
                cs.registerOutParameter(3, Types.REF_CURSOR);
                cs.execute();
                List<PortalDetalleDTO.PortalPasajeroDTO> lista = new ArrayList<>();
                try (ResultSet rs = cs.getObject(3, ResultSet.class)) {
                    while (rs.next()) {
                        lista.add(new PortalDetalleDTO.PortalPasajeroDTO(rs.getString("nombre_completo"),
                                rs.getString("tipo_documento"), rs.getString("numero_documento"),
                                rs.getString("nacionalidad") == null ? "" : rs.getString("nacionalidad")));
                    }
                }
                return lista;
            }
        });
        List<VistaPaqueteCliente> filas = vistaRepository.findByReservaId(reservaId);
        if (filas.isEmpty()) throw new AccesoDenegadoException("El paquete solicitado no pertenece a tu cuenta.");
        VistaPaqueteCliente v = filas.get(0);
        String descripcion = paqueteRepository.findById(v.getPaqueteId()).map(p -> p.getDescripcion() == null ? "" : p.getDescripcion()).orElse("");
        String guia = salidaRepository.findById(v.getSalidaId())
                .flatMap(s -> s.getGuiaId() == null ? java.util.Optional.empty() : personalRepository.findById(s.getGuiaId()))
                .map(g -> g.getNombreCompleto()).orElse(null);
        List<PagoDTO> pagos = pagoRepository.findByReservaIdOrderByPagoId(reservaId).stream().map(PagoDTO::desde).toList();
        return new PortalDetalleDTO(PortalPaqueteDTO.desde(v), descripcion, guia, pasajeros, pagos);
    }

    @Transactional
    public PagoDTO informarPago(Long reservaId, InformarPagoRequestDTO dto) {
        Long clienteId = clienteActual();
        // PKG_GESTION_PAQUETES_CLIENTE.iniciar_pago: valida dueño, estado y saldo; queda PENDIENTE
        Long pagoId = jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{call PKG_GESTION_PAQUETES_CLIENTE.iniciar_pago(?, ?, ?, ?, ?, ?)}")) {
                cs.setLong(1, clienteId);
                cs.setLong(2, reservaId);
                cs.setBigDecimal(3, dto.monto());
                cs.setString(4, dto.metodoPago().trim());
                cs.setString(5, dto.codigoTransaccion().trim());
                cs.registerOutParameter(6, Types.NUMERIC);
                cs.execute();
                return cs.getLong(6);
            }
        });
        Pago p = pagoRepository.findById(pagoId).orElseThrow(() -> new BusinessException("No se pudo registrar el pago."));
        // El INSERT lo hizo el paquete PL/SQL (no pasa por JPA): se audita aquí, en la misma transacción
        auditoria.registrar("CREAR", "PAGO", pagoId, "Pago informado desde el portal: reserva " + reservaId + ", S/ "
                + dto.monto().toPlainString() + " por " + dto.metodoPago().trim() + " (op. " + dto.codigoTransaccion().trim()
                + "), queda PENDIENTE de validación");
        return PagoDTO.desde(p);
    }

    private Long clienteActual() {
        SesionUsuario s = SesionActual.get();
        if (s == null || !s.esCliente() || s.clienteId() == null) {
            throw new AccesoDenegadoException("Esta sección es solo para clientes.");
        }
        return s.clienteId();
    }
}
