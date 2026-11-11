package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.ProveedorDTO;
import pe.edu.upeu.lp2_gp2.dto.ProveedorRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.Reglas;
import pe.edu.upeu.lp2_gp2.entity.OperacionTransporte;
import pe.edu.upeu.lp2_gp2.entity.Personal;
import pe.edu.upeu.lp2_gp2.entity.Proveedor;
import pe.edu.upeu.lp2_gp2.entity.Vehiculo;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.DuplicateResourceException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.OperacionTransporteRepository;
import pe.edu.upeu.lp2_gp2.repository.PersonalRepository;
import pe.edu.upeu.lp2_gp2.repository.ProveedorRepository;
import pe.edu.upeu.lp2_gp2.repository.VehiculoRepository;

import pe.edu.upeu.lp2_gp2.seguridad.SesionActual;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Proveedores: Kampu Tours (es_propio = 1, único) y terceros. Tanto la flota propia
 * como la de terceros cuelgan de un proveedor, así no hace falta inventar una
 * "empresa de transporte" para registrar los vehículos de Kampu.
 * Afiliación: una empresa se registra desde la web (estado PENDIENTE) y Kampu la aprueba
 * (ACTIVO). Solo las empresas ACTIVAS reciben servicios. Un usuario PROVEEDOR ve y
 * edita únicamente los datos de su empresa (no puede cambiar su estado).
 */
@Service
public class ProveedorServiceImpl implements IProveedorService {
    private final ProveedorRepository proveedorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PersonalRepository personalRepository;
    private final OperacionTransporteRepository operacionRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository, VehiculoRepository vehiculoRepository,
                                PersonalRepository personalRepository, OperacionTransporteRepository operacionRepository) {
        this.proveedorRepository = proveedorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.personalRepository = personalRepository;
        this.operacionRepository = operacionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDTO> listarTodo() {
        Datos d = datos();
        Long propio = SesionActual.proveedorId();
        return proveedorRepository.findAllByOrderByEsPropioDescProveedorIdAsc().stream()
                .filter(p -> propio == null || propio.equals(p.getProveedorId()))
                .map(p -> dto(p, d)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorDTO buscarPorId(Long id) {
        SesionActual.verificarProveedor(id);
        return dto(obtener(id), datos());
    }

    @Override
    @Transactional
    public ProveedorDTO crear(ProveedorRequestDTO dto) {
        soloKampu();
        Proveedor p = new Proveedor();
        copiar(dto, p, null);
        p.setFechaAfiliacion(LocalDate.now());
        return dto(proveedorRepository.saveAndFlush(p), datos());
    }

    @Override
    @Transactional
    public Proveedor afiliar(ProveedorRequestDTO dto) {
        Proveedor p = new Proveedor();
        copiar(dto, p, null);
        p.setEsPropio(0);
        p.setEstado("PENDIENTE");
        p.setFechaAfiliacion(LocalDate.now());
        return proveedorRepository.saveAndFlush(p);
    }

    @Override
    @Transactional
    public ProveedorDTO actualizar(Long id, ProveedorRequestDTO dto) {
        SesionActual.verificarProveedor(id);
        Proveedor p = obtener(id);
        String estado = p.getEstado();
        Integer esPropio = p.getEsPropio();
        copiar(dto, p, id);
        if (SesionActual.proveedorId() != null) {   // la empresa no cambia su propio estado ni su tipo
            p.setEstado(estado);
            p.setEsPropio(esPropio);
        }
        return dto(proveedorRepository.saveAndFlush(p), datos());
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        soloKampu();
        Proveedor p = obtener(id);
        if (vehiculoRepository.existsByProveedorId(id) || personalRepository.existsByProveedorId(id)) {
            throw new BusinessException("El proveedor tiene vehículos o personal registrados. Puedes marcarlo como INACTIVO.");
        }
        proveedorRepository.delete(p);
    }

    private void soloKampu() {
        if (SesionActual.proveedorId() != null) {
            throw new pe.edu.upeu.lp2_gp2.exception.AccesoDenegadoException("Solo Kampu Tours registra o elimina empresas proveedoras.");
        }
    }

    // Flota, personal y operaciones cargados una vez para calcular la disponibilidad de todos
    private record Datos(List<Vehiculo> vehiculos, List<Personal> personal, List<OperacionTransporte> operaciones) {
    }

    private Datos datos() {
        return new Datos(vehiculoRepository.findAll(), personalRepository.findAll(), operacionRepository.findAll());
    }

    private ProveedorDTO dto(Proveedor p, Datos d) {
        int flota = 0, capacidad = 0, conductores = 0, guias = 0, disponibles = 0, hoy = 0, porConfirmar = 0;
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate fecha = ahora.toLocalDate();
        Set<Long> propios = new HashSet<>();
        for (Vehiculo v : d.vehiculos()) {
            if (!p.getProveedorId().equals(v.getProveedorId())) continue;
            propios.add(v.getVehiculoId());
            flota++;
            if ("OPERATIVO".equals(v.getEstado())) capacidad += v.getCapacidadPax();
        }
        Set<Long> ocupados = new HashSet<>();
        for (OperacionTransporte o : d.operaciones()) {
            if (o.getVehiculoId() == null || !propios.contains(o.getVehiculoId())) continue;
            EstadoOperacion e = EstadoOperacion.de(o.getEstado());
            if (e == EstadoOperacion.CANCELADO) continue;
            if (o.getHoraSalida().toLocalDate().equals(fecha)) hoy++;
            if ("PENDIENTE".equals(o.getConfirmacion()) && !e.esFinal()) porConfirmar++;
            if (!e.esFinal() && !o.getHoraSalida().isAfter(ahora) && o.getHoraLlegada().isAfter(ahora)) ocupados.add(o.getVehiculoId());
        }
        for (Vehiculo v : d.vehiculos()) {
            if (propios.contains(v.getVehiculoId()) && "OPERATIVO".equals(v.getEstado()) && !ocupados.contains(v.getVehiculoId())) disponibles++;
        }
        for (Personal x : d.personal()) {
            if (!p.getProveedorId().equals(x.getProveedorId()) || "INACTIVO".equals(x.getEstado())) continue;
            if (!"GUIA".equals(x.getRol())) conductores++;
            if (!"CONDUCTOR".equals(x.getRol())) guias++;
        }
        return ProveedorDTO.desde(p, flota, capacidad, conductores, guias, disponibles, hoy, porConfirmar);
    }

    private Proveedor obtener(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con Id: " + id));
    }

    private void copiar(ProveedorRequestDTO dto, Proveedor p, Long id) {
        if (!Reglas.telefonoValido(dto.telefono())) throw new BusinessException(Reglas.MSG_TELEFONO);
        String ruc = dto.ruc().trim();
        Optional<Proveedor> mismo = proveedorRepository.findByRuc(ruc);
        if (mismo.isPresent() && !mismo.get().getProveedorId().equals(id)) {
            throw new DuplicateResourceException("Ya existe un proveedor con el RUC " + ruc + ".");
        }
        if (dto.telefono() != null && !dto.telefono().isBlank()
                && proveedorRepository.contarTelefono(Reglas.digitos(dto.telefono()), Reglas.idOMenosUno(id)) > 0) {
            throw new DuplicateResourceException("El teléfono " + dto.telefono().trim() + " ya está registrado para otro proveedor.");
        }
        if (dto.esPropio()) {
            Optional<Proveedor> propio = proveedorRepository.findFirstByEsPropio(1);
            if (propio.isPresent() && !propio.get().getProveedorId().equals(id)) {
                throw new BusinessException("Ya existe el proveedor propio (" + propio.get().getNombreComercial() + ").");
            }
        } else if (id != null && Integer.valueOf(1).equals(p.getEsPropio())) {
            throw new BusinessException("Kampu Tours debe seguir siendo el proveedor propio.");
        }
        if (dto.esPropio() && !"ACTIVO".equals(dto.estado()) && !"INACTIVO".equals(dto.estado())) {
            throw new BusinessException("Kampu Tours no pasa por afiliación: su estado es ACTIVO o INACTIVO.");
        }
        p.setNombreComercial(dto.nombreComercial().trim());
        p.setRuc(ruc);
        p.setTelefono(vacio(dto.telefono()));
        p.setEsPropio(dto.esPropio() ? 1 : 0);
        p.setEstado(dto.estado());
        p.setDireccion(vacio(dto.direccion()));
        p.setEmail(vacio(dto.email()));
        p.setContacto(vacio(dto.contacto()));
        // Orden fijo y sin repetidos: "TRASLADO,TOUR,INTERPROVINCIAL"
        p.setServicios(List.of("TRASLADO", "TOUR", "INTERPROVINCIAL").stream()
                .filter(dto.servicios()::contains).collect(Collectors.joining(",")));
        p.setZonas(zonas(dto.zonas()));
    }

    // "cusco, Puno ,cusco" -> "Cusco,Puno"
    private static String zonas(String texto) {
        if (texto == null || texto.isBlank()) return null;
        LinkedHashMap<String, String> unicas = new LinkedHashMap<>();
        for (String z : texto.split(",")) {
            String limpia = z.trim().replaceAll("\\s+", " ");
            if (limpia.isEmpty()) continue;
            unicas.putIfAbsent(limpia.toLowerCase(), Character.toUpperCase(limpia.charAt(0)) + limpia.substring(1));
        }
        return unicas.isEmpty() ? null : String.join(",", unicas.values());
    }

    private static String vacio(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
