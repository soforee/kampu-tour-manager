package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.auditoria.AuditoriaService;
import pe.edu.upeu.lp2_gp2.auditoria.RegistroAuditoria;
import pe.edu.upeu.lp2_gp2.exception.NoAutenticadoException;
import pe.edu.upeu.lp2_gp2.repository.PkgSeguridadRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.*;
import pe.edu.upeu.lp2_gp2.entity.Cliente;
import pe.edu.upeu.lp2_gp2.entity.Proveedor;
import pe.edu.upeu.lp2_gp2.entity.UsuarioSistema;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.repository.ClienteRepository;
import pe.edu.upeu.lp2_gp2.repository.ProveedorRepository;
import pe.edu.upeu.lp2_gp2.repository.UsuarioSistemaRepository;
import pe.edu.upeu.lp2_gp2.seguridad.SesionUsuario;
import pe.edu.upeu.lp2_gp2.seguridad.TokenService;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.Optional;

/**
 * Login único para el personal interno (USUARIOS_SISTEMA, por usuario) y para los clientes
 * del portal (CLIENTES, por correo o documento). El registro de clientes pasa por
 * SP_REGISTRAR_CLIENTE_EXTERNO; la contraseña se guarda como hash BCrypt.
 */
@Service
public class AuthServiceImpl implements IAuthService {
    private static final String CREDENCIALES = "Usuario o contraseña incorrectos.";

    private final UsuarioSistemaRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final TokenService tokens;
    private final JdbcTemplate jdbc;
    private final PkgSeguridadRepository seguridad;
    private final AuditoriaService auditoria;
    private final ProveedorRepository proveedorRepository;
    private final IProveedorService proveedorService;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public AuthServiceImpl(UsuarioSistemaRepository usuarioRepository, ClienteRepository clienteRepository,
                           TokenService tokens, JdbcTemplate jdbc, PkgSeguridadRepository seguridad,
                           AuditoriaService auditoria, ProveedorRepository proveedorRepository,
                           IProveedorService proveedorService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.tokens = tokens;
        this.jdbc = jdbc;
        this.seguridad = seguridad;
        this.auditoria = auditoria;
        this.proveedorRepository = proveedorRepository;
        this.proveedorService = proveedorService;
    }

    // Inicio de sesión auditado: LOGIN si entra, LOGIN_FALLIDO (con el motivo) si no.
    // Se registran en transacción propia: el login es de solo lectura y el fallido termina en excepción.
    @Override
    @Transactional(readOnly = true)
    public SesionDTO login(LoginRequestDTO dto) {
        String id = dto.username().trim();
        try {
            SesionDTO s = autenticar(id, dto.password());
            auditoria.registrarIndependiente(new RegistroAuditoria("LOGIN", "SESION",
                    s.usuarioId() != null ? s.usuarioId() : s.clienteId(), null, null, null,
                    "Inicio de sesión (" + s.rol() + ")", s.username(), s.rol()));
            return s;
        } catch (BusinessException ex) {
            auditoria.registrarIndependiente(new RegistroAuditoria("LOGIN_FALLIDO", "SESION", null, null, null, null,
                    ex.getMessage(), id, null));
            throw ex;
        }
    }

    private SesionDTO autenticar(String id, String password) {
        LoginRequestDTO dto = new LoginRequestDTO(id, password);
        // 1) Personal interno
        Optional<UsuarioSistema> usuario = usuarioRepository.findByUsernameIgnoreCase(id);
        if (usuario.isPresent()) {
            UsuarioSistema u = usuario.get();
            if (!coincide(dto.password(), u.getPasswordHash())) throw new BusinessException(CREDENCIALES);
            if (!"ACTIVO".equals(u.getEstado())) throw new BusinessException("Tu usuario está inactivo. Contacta al administrador.");
            String nombre = u.getUsername();
            if (u.getProveedorId() != null) {   // empresa proveedora: entra salvo que Kampu la haya dado de baja
                Proveedor p = proveedorRepository.findById(u.getProveedorId()).orElseThrow(() -> new BusinessException(CREDENCIALES));
                if ("INACTIVO".equals(p.getEstado())) throw new BusinessException("Tu empresa está inactiva en Kampu Tours. Contacta al centro de operaciones.");
                nombre = p.getNombreComercial();
            }
            return dto(tokens.crear(u.getUsuarioId(), null, u.getUsername(), nombre, u.getRol(), u.getProveedorId()), u.getEstado());
        }
        // 2) Cliente del portal (correo o número de documento)
        Optional<Cliente> cliente = id.contains("@")
                ? clienteRepository.findFirstByEmailIgnoreCase(id)
                : clienteRepository.findFirstByNumeroDocumentoIgnoreCase(id);
        if (cliente.isEmpty()) throw new BusinessException(CREDENCIALES);
        Cliente c = cliente.get();
        if (c.getPasswordHash() == null) {
            throw new BusinessException("Tu cuenta web aún no está activada. Usa \"Registrarse\" con tu mismo documento y correo.");
        }
        if (!bcrypt.matches(dto.password(), c.getPasswordHash())) throw new BusinessException(CREDENCIALES);
        if (!"ACTIVO".equals(c.getEstadoCuenta())) throw new BusinessException("Tu cuenta está " + c.getEstadoCuenta().toLowerCase() + ". Contacta a la agencia.");
        return dto(tokens.crear(null, c.getClienteId(), c.getEmail(), c.getNombreCompleto(), "CLIENTE", null), c.getEstadoCuenta());
    }

    @Override
    @Transactional
    public SesionDTO registrar(RegistroRequestDTO dto) {
        if (!dto.password().equals(dto.confirmacion())) throw new BusinessException("Las contraseñas no coinciden.");
        if (dto.password().isBlank() || dto.password().trim().length() < 8) {
            throw new BusinessException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (!Reglas.documentoValido(dto.tipoDocumento(), dto.numeroDocumento())) throw new BusinessException(Reglas.MSG_DOCUMENTO);
        String errorTelefono = Reglas.errorTelefonoPais(dto.telefono());
        if (errorTelefono != null) throw new BusinessException(errorTelefono);
        String hash = bcrypt.encode(dto.password());

        // SP_REGISTRAR_CLIENTE_EXTERNO: valida correo/documento/teléfono, guarda y audita
        Long clienteId = jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{call SP_REGISTRAR_CLIENTE_EXTERNO(?, ?, ?, ?, ?, ?, ?)}")) {
                cs.setString(1, dto.nombreCompleto().trim());
                cs.setString(2, dto.tipoDocumento());
                cs.setString(3, dto.numeroDocumento().trim());
                cs.setString(4, dto.email().trim());
                cs.setString(5, dto.telefono().trim());
                cs.setString(6, hash);
                cs.registerOutParameter(7, Types.NUMERIC);
                cs.execute();
                return cs.getLong(7);
            }
        });
        Cliente c = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new BusinessException("No se pudo completar el registro."));
        // Autenticación automática tras registrarse
        return dto(tokens.crear(null, c.getClienteId(), c.getEmail(), c.getNombreCompleto(), "CLIENTE", null), "ACTIVO");
    }

    // Afiliación de una empresa de transporte: queda PENDIENTE hasta que Kampu la apruebe
    @Override
    @Transactional
    public SesionDTO afiliar(AfiliacionRequestDTO dto) {
        if (!dto.password().equals(dto.confirmacion())) throw new BusinessException("Las contraseñas no coinciden.");
        if (dto.password().isBlank() || dto.password().trim().length() < 8) {
            throw new BusinessException("La contraseña debe tener al menos 8 caracteres.");
        }
        String errorTelefono = Reglas.errorTelefonoPais(dto.telefono());
        if (errorTelefono != null) throw new BusinessException(errorTelefono);
        String username = dto.username().trim().toLowerCase();
        if (usuarioRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new pe.edu.upeu.lp2_gp2.exception.DuplicateResourceException("El usuario " + username + " ya existe: elige otro.");
        }
        Proveedor p = proveedorService.afiliar(new ProveedorRequestDTO(dto.nombreComercial(), dto.ruc(), dto.telefono(), false,
                "PENDIENTE", dto.direccion(), dto.email(), dto.contacto(), dto.servicios(), dto.zonas()));
        UsuarioSistema u = new UsuarioSistema();
        u.setUsername(username);
        u.setPasswordHash(bcrypt.encode(dto.password()));
        u.setRol("PROVEEDOR");
        u.setEstado("ACTIVO");
        u.setProveedorId(p.getProveedorId());
        usuarioRepository.saveAndFlush(u);
        auditoria.registrarIndependiente(new RegistroAuditoria("REGISTRO", "PROVEEDORES", p.getProveedorId(), null, null, null,
                "Solicitud de afiliación de " + p.getNombreComercial() + " (RUC " + p.getRuc() + ")", username, "PROVEEDOR"));
        return dto(tokens.crear(u.getUsuarioId(), null, username, p.getNombreComercial(), "PROVEEDOR", p.getProveedorId()), "ACTIVO");
    }

    @Override
    public void logout(String token) {
        SesionUsuario s = tokens.obtener(token);
        tokens.eliminar(token);
        if (s != null) {
            auditoria.registrarIndependiente(new RegistroAuditoria("LOGOUT", "SESION",
                    s.usuarioId() != null ? s.usuarioId() : s.clienteId(), null, null, null,
                    "Cierre de sesión", s.username(), s.rol()));
        }
    }

    @Override
    public SesionDTO sesion(String token) {
        SesionUsuario s = tokens.obtener(token);
        if (s == null) throw new NoAutenticadoException("Tu sesión venció. Vuelve a iniciar sesión.");
        return dto(s, "ACTIVO");
    }

    // Personal interno: hash BCrypt o (datos de demostración) texto plano
    private boolean coincide(String clave, String guardada) {
        if (guardada == null) return false;
        return guardada.startsWith("$2") ? bcrypt.matches(clave, guardada) : guardada.equals(clave);
    }

    private SesionDTO dto(SesionUsuario s, String estado) {
        Proveedor p = s.proveedorId() == null ? null : proveedorRepository.findById(s.proveedorId()).orElse(null);
        return new SesionDTO(s.usuarioId(), s.clienteId(), s.username(), s.nombre(), s.rol(),
                p == null ? null : p.getProveedorId(), p == null ? null : p.getNombreComercial(), p == null ? null : p.getEstado(),
                estado, s.token(),
                seguridad.permisos(s.rol()));
    }
}
