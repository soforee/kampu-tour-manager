package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.*;
import pe.edu.upeu.lp2_gp2.entity.*;
import pe.edu.upeu.lp2_gp2.exception.AccesoDenegadoException;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.*;
import pe.edu.upeu.lp2_gp2.seguridad.SesionActual;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tráfico de transporte: centro de operaciones con varias empresas (multi-proveedor).
 *  - Transiciones de estado: EstadoOperacion (incluye EN_ESPERA_SALIDA, LLEGADA_PROXIMA y RETRASADO).
 *  - Reglas obligatorias (también en la asignación manual): vehículo y conductor del MISMO proveedor,
 *    proveedor ACTIVO, vehículo OPERATIVO con capacidad, del tipo exigido y sin mantenimiento antes
 *    de la llegada, conductor DISPONIBLE con licencia vigente y de la categoría que pide el vehículo,
 *    sin cruces de horario (30 min de margen), tiempo para reposicionarse desde el servicio anterior
 *    y hacia el siguiente, y máximo 10 h de conducción por día.
 *  - Reglas de prioridad (solo la asignación automática): empresa preferida, servicios y zonas que
 *    atiende la empresa, empresas que ya rechazaron el servicio; luego flota propia
 *    (si no tiene que viajar vacía muchas horas), menos reposicionamiento, menos asientos vacíos,
 *    menos horas del conductor y menos servicios del vehículo en el día.
 *  - Confirmación: la flota propia queda ACEPTADA; a un tercero se le asigna PENDIENTE y debe aceptar
 *    o rechazar. Sin aceptación no hay embarque. Si rechaza, la operación vuelve a la cola.
 *  - Embarque: el tramo debe tener asientos para todos los pasajeros de la salida.
 *  - Salida CONFIRMADA: no se le puede quitar transporte que haga falta.
 *  - Un usuario PROVEEDOR solo ve y responde los servicios asignados a su empresa.
 *  - Cada cambio queda en HISTORIAL_OPERACIONES con el usuario de la sesión.
 */
@Service
public class OperacionTransporteServiceImpl implements IOperacionTransporteService {
    private static final int MARGEN_MIN = 30;
    private static final double MAX_HORAS_DIA = 10;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String CANCELADO = EstadoOperacion.CANCELADO.name();
    // Categoría mínima de licencia (MTC) por tipo de vehículo
    private static final List<String> CATEGORIAS = List.of("A-I", "A-IIa", "A-IIb", "A-IIIa", "A-IIIb", "A-IIIc");
    private static final Map<String, String> LICENCIA_MINIMA = Map.of(
            "AUTO", "A-I", "CAMIONETA_4X4", "A-I", "VAN", "A-IIb", "MINIBUS", "A-IIIa", "BUS", "A-IIIb");

    private final OperacionTransporteRepository operacionRepository;
    private final HistorialOperacionRepository historialRepository;
    private final SalidaProgramadaRepository salidaRepository;
    private final LugarRepository lugarRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PersonalRepository personalRepository;
    private final ProveedorRepository proveedorRepository;
    private final CapacidadTransporte capacidad;
    private final DuracionRutaService duracion;

    public OperacionTransporteServiceImpl(OperacionTransporteRepository operacionRepository,
                                          HistorialOperacionRepository historialRepository,
                                          SalidaProgramadaRepository salidaRepository, LugarRepository lugarRepository,
                                          VehiculoRepository vehiculoRepository, PersonalRepository personalRepository,
                                          ProveedorRepository proveedorRepository, CapacidadTransporte capacidad,
                                          DuracionRutaService duracion) {
        this.operacionRepository = operacionRepository;
        this.historialRepository = historialRepository;
        this.salidaRepository = salidaRepository;
        this.lugarRepository = lugarRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.personalRepository = personalRepository;
        this.proveedorRepository = proveedorRepository;
        this.capacidad = capacidad;
        this.duracion = duracion;
    }

    // ---------- Consultas ----------

    @Override
    @Transactional(readOnly = true)
    public List<OperacionDTO> listar(Long salidaId) {
        List<OperacionTransporte> lista = salidaId == null
                ? operacionRepository.findAllByOrderByHoraSalidaAscOperacionIdAsc()
                : operacionRepository.findBySalidaIdOrderByHoraSalidaAscOperacionIdAsc(salidaId);
        Contexto ctx = contexto();
        Long propio = SesionActual.proveedorId();
        return lista.stream()
                .filter(o -> propio == null || propio.equals(proveedorDe(o, ctx)))
                .map(o -> dto(o, ctx)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OperacionDTO buscarPorId(Long id) {
        OperacionTransporte o = obtener(id);
        verificarAlcance(o);
        return dto(o, contexto());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialOperacionDTO> historial(Long id) {
        verificarAlcance(obtener(id));
        List<HistorialOperacionDTO> lista = new ArrayList<>();
        String anterior = null;
        for (HistorialOperacion h : historialRepository.findByOperacionIdOrderByHistorialIdAsc(id)) {
            lista.add(new HistorialOperacionDTO(h.getHistorialId(), h.getOperacionId(), anterior, h.getEstadoNuevo(),
                    h.getFechaCambio(), h.getUsuario(), h.getObservacion() == null ? "" : h.getObservacion()));
            anterior = h.getEstadoNuevo();
        }
        return lista;
    }

    @Override
    public List<EstadoOperacionDTO> estados() {
        return Arrays.stream(EstadoOperacion.values())
                .map(e -> new EstadoOperacionDTO(e.name(), e.nombre(), e.orden(), e.requiereRecursos(), e.ocupaRecursos(),
                        e.esFinal(), e.siguientesOrdenados()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EstimacionDuracionDTO estimarDuracion(Long origenId, Long destinoId, String tipoServicio) {
        return duracion.estimar(origenId, destinoId, tipoServicio);
    }

    /** Todos los vehículos y conductores, con el motivo por el que no son elegibles (null = elegible) */
    @Override
    @Transactional(readOnly = true)
    public List<RecursoCandidatoDTO> candidatos(Long id) {
        soloKampu();
        OperacionTransporte o = obtener(id);
        Map<Long, Proveedor> proveedores = proveedores();
        Lugar origen = lugarRepository.findById(o.getOrigenId()).orElse(null);
        List<RecursoCandidatoDTO> lista = new ArrayList<>();
        for (Vehiculo v : vehiculoRepository.findAllByOrderByVehiculoIdAsc()) {
            Proveedor p = proveedores.get(v.getProveedorId());
            lista.add(new RecursoCandidatoDTO("VEHICULO", v.getVehiculoId(), p.getProveedorId(), p.getNombreComercial(),
                    esPropio(p), v.getPlaca() + " · " + (v.getMarcaModelo() == null ? v.getTipoVehiculo() : v.getMarcaModelo())
                    + " · " + v.getTipoVehiculo(),
                    v.getCapacidadPax(), null, motivoVehiculo(v, p, o), advertencia(p, o, origen)));
        }
        for (Personal c : personalRepository.findAllByOrderByPersonalIdAsc()) {
            if ("GUIA".equals(c.getRol())) continue;
            Proveedor p = proveedores.get(c.getProveedorId());
            lista.add(new RecursoCandidatoDTO("CONDUCTOR", c.getPersonalId(), p.getProveedorId(), p.getNombreComercial(),
                    esPropio(p), c.getNombreCompleto() + " (" + c.getCatLicencia() + ")", null,
                    BigDecimal.valueOf(horasDelDia(c.getPersonalId(), o)).setScale(1, RoundingMode.HALF_UP),
                    motivoConductor(c, p, o), advertencia(p, o, origen)));
        }
        // Elegibles primero; luego sin advertencias; dentro de ellos, la flota propia primero
        lista.sort(Comparator.comparing((RecursoCandidatoDTO r) -> r.motivo() != null)
                .thenComparing(r -> r.advertencia() != null)
                .thenComparing(r -> !r.proveedorPropio())
                .thenComparing(RecursoCandidatoDTO::proveedor));
        return lista;
    }

    // ---------- Escritura ----------

    @Override
    @Transactional
    public OperacionDTO crear(OperacionRequestDTO dto) {
        OperacionTransporte o = new OperacionTransporte();
        copiar(dto, o);
        if (o.getHoraSalida().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BusinessException("La hora de salida ya pasó: programa el servicio a futuro.");
        }
        o.setCodigoOperacion(String.format("KMP-%03d", operacionRepository.siguienteCodigo()));
        o.setEstado(EstadoOperacion.PROGRAMADO.name());
        if (o.getVehiculoId() != null) {
            validarRecursos(o);
            o.setEstado(EstadoOperacion.ASIGNADO.name());
            o.setConfirmacion(confirmacionInicial(o.getVehiculoId()));
        }
        OperacionTransporte guardada = operacionRepository.saveAndFlush(o);
        registrar(guardada, o.getVehiculoId() != null ? "Asignación manual al programar" : null);
        return dto(guardada, contexto());
    }

    @Override
    @Transactional
    public OperacionDTO actualizar(Long id, OperacionRequestDTO dto) {
        OperacionTransporte o = obtener(id);
        EstadoOperacion actual = EstadoOperacion.de(o.getEstado());
        if (actual.esFinal()) throw new BusinessException(o.getCodigoOperacion() + " está " + actual.name() + " y no se puede modificar.");
        Long vehiculoAntes = o.getVehiculoId();
        Long conductorAntes = o.getConductorId();
        copiar(dto, o);
        boolean conRecursos = o.getVehiculoId() != null;
        if (!actual.antesDeSalir() && (!Objects.equals(vehiculoAntes, o.getVehiculoId()) || !Objects.equals(conductorAntes, o.getConductorId()))) {
            throw new BusinessException(o.getCodigoOperacion() + " ya está en " + actual.nombre().toLowerCase()
                    + ": no se puede cambiar el vehículo ni el conductor.");
        }
        EstadoOperacion nuevo = actual;
        if (conRecursos && actual == EstadoOperacion.PROGRAMADO) nuevo = EstadoOperacion.ASIGNADO;
        if (!conRecursos && actual == EstadoOperacion.ASIGNADO) nuevo = EstadoOperacion.PROGRAMADO;
        if (nuevo.requiereRecursos() && !conRecursos) {
            throw new BusinessException("El estado " + nuevo.name() + " requiere vehículo y conductor asignados.");
        }
        if (conRecursos) validarRecursos(o);
        if (!conRecursos) o.setConfirmacion(null);
        else if (!Objects.equals(vehiculoAntes, o.getVehiculoId())) o.setConfirmacion(confirmacionInicial(o.getVehiculoId()));
        o.setEstado(nuevo.name());
        operacionRepository.saveAndFlush(o);
        validarSalidaConfirmada(o.getSalidaId());
        if (nuevo != actual || !Objects.equals(vehiculoAntes, o.getVehiculoId())) registrar(o, "Cambio por edición de la operación");
        return dto(o, contexto());
    }

    @Override
    @Transactional
    public OperacionDTO cambiarEstado(Long id, CambioEstadoRequestDTO dto) {
        OperacionTransporte o = obtener(id);
        EstadoOperacion actual = EstadoOperacion.de(o.getEstado());
        EstadoOperacion nuevo = EstadoOperacion.de(dto.estado());
        boolean reprogramar = actual == EstadoOperacion.RETRASADO && nuevo == EstadoOperacion.RETRASADO;
        if (actual == nuevo && !reprogramar) throw new BusinessException(o.getCodigoOperacion() + " ya está en estado " + nuevo.name() + ".");
        if (!reprogramar && !actual.puedePasarA(nuevo)) {
            throw new BusinessException("Transición no permitida: " + actual.nombre() + " → " + nuevo.nombre());
        }
        String observacion = dto.observacion();
        if (nuevo == EstadoOperacion.RETRASADO) {
            observacion = retrasar(o, dto.nuevaHoraSalida(), observacion);
        }
        if (nuevo == EstadoOperacion.PROGRAMADO) {   // desasignar: libera vehículo y conductor
            o.setVehiculoId(null);
            o.setConductorId(null);
            o.setConfirmacion(null);
        }
        if (nuevo.requiereRecursos() && (o.getVehiculoId() == null || o.getConductorId() == null)) {
            throw new BusinessException("El estado " + nuevo.nombre() + " requiere vehículo y conductor asignados.");
        }
        if ((nuevo == EstadoOperacion.EMBARQUE || nuevo == EstadoOperacion.EN_ESPERA_SALIDA || nuevo == EstadoOperacion.EN_RUTA)
                && !"ACEPTADO".equals(o.getConfirmacion())) {
            throw new BusinessException(o.getCodigoOperacion() + ": la empresa de transporte aún no confirma el servicio.");
        }
        o.setEstado(nuevo.name());
        operacionRepository.saveAndFlush(o);
        if (nuevo == EstadoOperacion.EMBARQUE && o.getSalidaId() != null) {
            SalidaProgramada s = salidaRepository.findById(o.getSalidaId()).orElseThrow();
            String falta = capacidad.faltante(s.getSalidaId(), s.getCuposMaximos() - s.getCuposDisponibles(), o);
            if (falta != null) throw new BusinessException(o.getCodigoOperacion() + " no puede embarcar: " + falta);
        }
        validarSalidaConfirmada(o.getSalidaId());
        registrar(o, observacion);
        return dto(o, contexto());
    }

    // Retraso: nueva hora de salida; la llegada estimada se corre lo mismo. Devuelve la observación del historial.
    private String retrasar(OperacionTransporte o, LocalDateTime nueva, String observacion) {
        if (nueva == null) throw new BusinessException("Indica la nueva hora de salida del servicio retrasado.");
        nueva = nueva.withSecond(0).withNano(0);
        if (!nueva.isAfter(o.getHoraSalida())) {
            throw new BusinessException("La nueva hora de salida debe ser posterior a la programada ("
                    + o.getHoraSalida().format(HORA) + ").");
        }
        LocalDateTime anterior = salidaEfectiva(o);
        o.setHoraLlegada(o.getHoraLlegada().plusMinutes(Duration.between(anterior, nueva).toMinutes()));
        o.setHoraSalidaEstimada(nueva);
        String nota = "Nueva salida " + nueva.format(HORA) + " (+" + Duration.between(o.getHoraSalida(), nueva).toMinutes() + " min)";
        return observacion == null || observacion.isBlank() ? nota : (nota + ". " + observacion.trim());
    }

    @Override
    @Transactional
    public OperacionDTO asignarAutomatico(Long id) {
        OperacionTransporte o = obtener(id);
        exigirSinRecursos(o);
        StringBuilder porque = new StringBuilder();
        Par mejor = mejorPar(o, porque);
        if (mejor == null) throw new BusinessException("Sin asignación para " + o.getCodigoOperacion() + ": " + porque);
        aplicar(o, mejor.vehiculo(), mejor.conductor(), "Asignación automática");
        return dto(o, contexto());
    }

    /** Asigna en orden de salida todas las operaciones futuras que aún no tienen vehículo */
    @Override
    @Transactional
    public AsignacionMasivaDTO asignarPendientes(LocalDate fecha) {
        List<OperacionTransporte> pendientes = operacionRepository.findByVehiculoIdIsNullAndEstadoInAndHoraSalidaGreaterThanEqualOrderByHoraSalidaAsc(
                List.of(EstadoOperacion.PROGRAMADO.name(), EstadoOperacion.RETRASADO.name()), LocalDateTime.now().minusMinutes(5));
        if (fecha != null) pendientes = pendientes.stream().filter(o -> o.getHoraSalida().toLocalDate().equals(fecha)).toList();
        List<OperacionTransporte> asignadas = new ArrayList<>();
        List<String> sinAsignar = new ArrayList<>();
        for (OperacionTransporte o : pendientes) {
            StringBuilder porque = new StringBuilder();
            Par mejor = mejorPar(o, porque);
            if (mejor == null) {
                sinAsignar.add(o.getCodigoOperacion() + ": " + porque);
                continue;
            }
            aplicar(o, mejor.vehiculo(), mejor.conductor(), "Asignación automática (lote)");
            asignadas.add(o);
        }
        Contexto ctx = contexto();
        return new AsignacionMasivaDTO(pendientes.size(), asignadas.stream().map(x -> dto(x, ctx)).toList(), sinAsignar);
    }

    /** Asignación manual (caso excepcional): exige las reglas obligatorias; las de prioridad quedan como advertencia */
    @Override
    @Transactional
    public OperacionDTO asignar(Long id, AsignacionRequestDTO dto) {
        OperacionTransporte o = obtener(id);
        exigirSinRecursos(o);
        o.setVehiculoId(dto.vehiculoId());
        o.setConductorId(dto.conductorId());
        validarRecursos(o);
        Vehiculo v = vehiculoRepository.findById(dto.vehiculoId()).orElseThrow();
        Proveedor p = proveedorRepository.findById(v.getProveedorId()).orElseThrow();
        String aviso = advertencia(p, o, lugarRepository.findById(o.getOrigenId()).orElse(null));
        aplicar(o, v, personalRepository.findById(dto.conductorId()).orElseThrow(),
                "Asignación manual" + (aviso == null ? "" : " (excepción: " + aviso + ")"));
        return dto(o, contexto());
    }

    /** La empresa (o el operador por ella) acepta o rechaza el servicio asignado */
    @Override
    @Transactional
    public OperacionDTO responder(Long id, RespuestaServicioRequestDTO dto) {
        OperacionTransporte o = obtener(id);
        verificarAlcance(o);
        EstadoOperacion e = EstadoOperacion.de(o.getEstado());
        if (o.getVehiculoId() == null || !"PENDIENTE".equals(o.getConfirmacion()) || !e.antesDeSalir()) {
            throw new BusinessException(o.getCodigoOperacion() + " no tiene una asignación pendiente de confirmar.");
        }
        Vehiculo v = vehiculoRepository.findById(o.getVehiculoId()).orElseThrow();
        Proveedor p = proveedorRepository.findById(v.getProveedorId()).orElseThrow();
        if (Boolean.TRUE.equals(dto.aceptar())) {
            o.setConfirmacion("ACEPTADO");
            operacionRepository.saveAndFlush(o);
            registrar(o, "Servicio aceptado por " + p.getNombreComercial());
            return dto(o, contexto());
        }
        String motivo = dto.motivo() == null ? "" : dto.motivo().trim();
        if (motivo.isEmpty()) throw new BusinessException("Indica el motivo del rechazo.");
        Set<String> rechazos = new LinkedHashSet<>(rechazos(o));
        rechazos.add(String.valueOf(p.getProveedorId()));
        o.setProveedoresRechazo(String.join(",", rechazos));
        o.setVehiculoId(null);
        o.setConductorId(null);
        o.setConfirmacion("RECHAZADO");
        o.setMotivoRechazo(motivo.length() > 200 ? motivo.substring(0, 200) : motivo);
        if (e == EstadoOperacion.ASIGNADO) o.setEstado(EstadoOperacion.PROGRAMADO.name());
        operacionRepository.saveAndFlush(o);
        // Sin validarSalidaConfirmada: el rechazo es un hecho; la operación vuelve a la cola para reasignarla
        registrar(o, abreviar("Rechazado por " + p.getNombreComercial() + ": " + motivo));
        return dto(o, contexto());
    }

    // ---------- Asignación ----------

    private record Par(Vehiculo vehiculo, Personal conductor) {
    }

    // Mejor par vehículo + conductor; null (y el porqué) si ninguno cumple
    private Par mejorPar(OperacionTransporte o, StringBuilder porque) {
        Map<Long, Proveedor> proveedores = proveedores();
        Lugar origen = lugarRepository.findById(o.getOrigenId()).orElse(null);
        List<Personal> conductores = personalRepository.findAllByOrderByPersonalIdAsc().stream()
                .filter(c -> !"GUIA".equals(c.getRol()))
                .filter(c -> motivoConductor(c, proveedores.get(c.getProveedorId()), o) == null).toList();
        Par mejor = null;
        double mejorPuntaje = Double.MAX_VALUE;
        int vehiculosAptos = 0;
        Map<String, Integer> motivos = new LinkedHashMap<>();
        for (Vehiculo v : vehiculoRepository.findAllByOrderByVehiculoIdAsc()) {
            Proveedor p = proveedores.get(v.getProveedorId());
            String motivo = motivoVehiculo(v, p, o);
            if (motivo == null) motivo = advertencia(p, o, origen);
            if (motivo != null) {
                motivos.merge(motivo.replaceAll(" con KMP-\\d+.*| desde KMP-\\d+.*| a KMP-\\d+.*| el \\d.*", ""), 1, Integer::sum);
                continue;
            }
            vehiculosAptos++;
            for (Personal c : conductores) {
                if (!c.getProveedorId().equals(v.getProveedorId()) || !licenciaPermite(c.getCatLicencia(), v.getTipoVehiculo())) continue;
                // Flota propia primero, salvo que tenga que viajar vacía más de ~5 h para llegar al origen
                double puntaje = (esPropio(p) ? 0 : 50_000)
                        + minutosReposicion(v.getVehiculoId(), o) * 150
                        + (v.getCapacidadPax() - o.getPaxProgramados()) * 100
                        + horasDelDia(c.getPersonalId(), o) * 10
                        + serviciosDelDia(v.getVehiculoId(), o) * 30;
                if (puntaje < mejorPuntaje) {
                    mejorPuntaje = puntaje;
                    mejor = new Par(v, c);
                }
            }
        }
        if (mejor == null) {
            if (vehiculosAptos > 0) {
                porque.append("hay ").append(vehiculosAptos).append(" vehículo(s) aptos pero ningún conductor libre de su misma empresa con la licencia requerida");
            } else {
                porque.append("ningún vehículo cumple (").append(motivos.entrySet().stream().limit(4)
                        .map(m -> m.getKey() + ": " + m.getValue()).collect(Collectors.joining("; "))).append(")");
            }
        }
        return mejor;
    }

    private void aplicar(OperacionTransporte o, Vehiculo v, Personal c, String nota) {
        o.setVehiculoId(v.getVehiculoId());
        o.setConductorId(c.getPersonalId());
        o.setConfirmacion(confirmacionInicial(v.getVehiculoId()));
        if (EstadoOperacion.PROGRAMADO.name().equals(o.getEstado())) o.setEstado(EstadoOperacion.ASIGNADO.name());
        operacionRepository.saveAndFlush(o);
        registrar(o, nota);
    }

    private void exigirSinRecursos(OperacionTransporte o) {
        EstadoOperacion e = EstadoOperacion.de(o.getEstado());
        if (o.getVehiculoId() != null || (e != EstadoOperacion.PROGRAMADO && e != EstadoOperacion.RETRASADO)) {
            throw new BusinessException(o.getCodigoOperacion() + " está " + e.nombre().toLowerCase()
                    + (o.getVehiculoId() != null ? " con vehículo" : "")
                    + ": para cambiar el vehículo o el conductor primero desasígnala (vuelve a PROGRAMADO).");
        }
    }

    // Flota propia: aceptado de inmediato. Tercero: debe confirmar.
    private String confirmacionInicial(Long vehiculoId) {
        Vehiculo v = vehiculoRepository.findById(vehiculoId).orElseThrow();
        Proveedor p = proveedorRepository.findById(v.getProveedorId()).orElseThrow();
        return esPropio(p) ? "ACEPTADO" : "PENDIENTE";
    }

    // ---------- Reglas ----------

    private void copiar(OperacionRequestDTO d, OperacionTransporte o) {
        if (d.salidaId() != null && !salidaRepository.existsById(d.salidaId())) throw new BusinessException("La salida seleccionada no existe.");
        if (!lugarRepository.existsById(d.origenId())) throw new BusinessException("El origen seleccionado no existe.");
        if (!lugarRepository.existsById(d.destinoId())) throw new BusinessException("El destino seleccionado no existe.");
        if (d.origenId().equals(d.destinoId())) throw new BusinessException("El origen y el destino deben ser distintos.");
        if ((d.vehiculoId() == null) != (d.conductorId() == null)) {
            throw new BusinessException("Asigna el vehículo y el conductor juntos (o deja ambos vacíos).");
        }
        if (d.proveedorPrefId() != null && !proveedorRepository.existsById(d.proveedorPrefId())) {
            throw new BusinessException("La empresa preferida no existe.");
        }
        String tipo = vacio(d.tipoServicio()) == null ? "TRASLADO" : d.tipoServicio();
        LocalDateTime salida = d.horaProgramadaSalida().withSecond(0).withNano(0);
        LocalDateTime llegada = d.horaEstimadaLlegada();
        if (llegada == null) {
            // La duración la calcula el backend con datos reales y se guarda en la operación
            Integer minutos = duracion.minutos(d.origenId(), d.destinoId(), tipo);
            if (minutos == null) {
                throw new BusinessException("No se puede calcular la llegada: registra las coordenadas del origen y del destino o indica la hora de llegada.");
            }
            llegada = salida.plusMinutes(minutos);
        }
        llegada = llegada.withSecond(0).withNano(0);
        if (!llegada.isAfter(salida)) throw new BusinessException("La llegada debe ser posterior a la hora de salida.");
        if (Duration.between(salida, llegada).toHours() > 24) throw new BusinessException("Un servicio no puede durar más de 24 horas.");
        o.setSalidaId(d.salidaId());
        o.setOrigenId(d.origenId());
        o.setDestinoId(d.destinoId());
        o.setVehiculoId(d.vehiculoId());
        o.setConductorId(d.conductorId());
        o.setPaxProgramados(d.paxProgramados());
        o.setHoraSalida(salida);
        o.setHoraLlegada(llegada);
        if (o.getHoraSalidaEstimada() != null && !o.getHoraSalidaEstimada().isAfter(salida)) o.setHoraSalidaEstimada(null);
        o.setObservaciones(vacio(d.observaciones()));
        o.setTipoServicio(tipo);
        o.setRuta(vacio(d.ruta()));
        o.setTipoVehiculoReq(vacio(d.tipoVehiculoReq()));
        o.setProveedorPrefId(d.proveedorPrefId());
    }

    private void validarRecursos(OperacionTransporte o) {
        Vehiculo v = vehiculoRepository.findById(o.getVehiculoId())
                .orElseThrow(() -> new BusinessException("El vehículo seleccionado no existe."));
        Personal c = personalRepository.findById(o.getConductorId())
                .orElseThrow(() -> new BusinessException("El conductor seleccionado no existe."));
        if (!v.getProveedorId().equals(c.getProveedorId())) {
            throw new BusinessException("El vehículo " + v.getPlaca() + " y el conductor " + c.getNombreCompleto()
                    + " deben ser del mismo proveedor.");
        }
        Proveedor p = proveedorRepository.findById(v.getProveedorId()).orElseThrow();
        String motivo = motivoVehiculo(v, p, o);
        if (motivo != null) throw new BusinessException(etiqueta(o) + " - vehículo " + v.getPlaca() + ": " + motivo);
        motivo = motivoConductor(c, p, o);
        if (motivo != null) throw new BusinessException(etiqueta(o) + " - conductor: " + motivo);
        if (!licenciaPermite(c.getCatLicencia(), v.getTipoVehiculo())) {
            throw new BusinessException(etiqueta(o) + " - conductor: su licencia " + c.getCatLicencia() + " no habilita a manejar "
                    + v.getTipoVehiculo() + " (mínimo " + LICENCIA_MINIMA.get(v.getTipoVehiculo()) + ").");
        }
    }

    // Reglas obligatorias del vehículo: null si puede atender la operación
    private String motivoVehiculo(Vehiculo v, Proveedor p, OperacionTransporte o) {
        if (!"ACTIVO".equals(p.getEstado())) return "proveedor " + p.getEstado();
        if (!"OPERATIVO".equals(v.getEstado())) return "vehículo en " + v.getEstado();
        if (o.getTipoVehiculoReq() != null && !o.getTipoVehiculoReq().equals(v.getTipoVehiculo())) {
            return "se requiere " + o.getTipoVehiculoReq();
        }
        if (o.getPaxProgramados() > v.getCapacidadPax()) {
            return "capacidad insuficiente (" + v.getCapacidadPax() + " < " + o.getPaxProgramados() + " pax)";
        }
        if (v.getProxMantenimiento() != null && !v.getProxMantenimiento().isAfter(o.getHoraLlegada().toLocalDate())) {
            return "mantenimiento programado el " + v.getProxMantenimiento().format(FECHA);
        }
        List<OperacionTransporte> cruces = operacionRepository.crucesVehiculo(v.getVehiculoId(), idOMenosUno(o),
                o.getHoraSalida().minusMinutes(MARGEN_MIN), o.getHoraLlegada().plusMinutes(MARGEN_MIN));
        if (!cruces.isEmpty()) return "cruce de horario con " + cruces.get(0).getCodigoOperacion();
        return motivoReposicion(v.getVehiculoId(), o);
    }

    // Tiempo para llegar al origen desde el servicio anterior y al servicio siguiente desde el destino
    private String motivoReposicion(Long vehiculoId, OperacionTransporte o) {
        OperacionTransporte antes = anterior(vehiculoId, o);
        if (antes != null && !antes.getDestinoId().equals(o.getOrigenId())) {
            Integer min = duracion.minutos(antes.getDestinoId(), o.getOrigenId(), "TRASLADO");
            if (min != null && antes.getHoraLlegada().plusMinutes(min + MARGEN_MIN).isAfter(o.getHoraSalida())) {
                return "no alcanza a reposicionarse desde " + antes.getCodigoOperacion() + " (" + min + " min de traslado)";
            }
        }
        OperacionTransporte despues = operacionRepository
                .findFirstByVehiculoIdAndEstadoNotAndOperacionIdNotAndHoraSalidaGreaterThanEqualOrderByHoraSalidaAsc(
                        vehiculoId, CANCELADO, idOMenosUno(o), o.getHoraLlegada()).orElse(null);
        if (despues != null && !o.getDestinoId().equals(despues.getOrigenId())) {
            Integer min = duracion.minutos(o.getDestinoId(), despues.getOrigenId(), "TRASLADO");
            if (min != null && o.getHoraLlegada().plusMinutes(min + MARGEN_MIN).isAfter(despues.getHoraSalida())) {
                return "no llega a tiempo a " + despues.getCodigoOperacion() + " (" + min + " min de traslado)";
            }
        }
        return null;
    }

    private OperacionTransporte anterior(Long vehiculoId, OperacionTransporte o) {
        return operacionRepository
                .findFirstByVehiculoIdAndEstadoNotAndOperacionIdNotAndHoraLlegadaLessThanEqualOrderByHoraLlegadaDesc(
                        vehiculoId, CANCELADO, idOMenosUno(o), o.getHoraSalida()).orElse(null);
    }

    // Minutos vacíos para llegar al origen (desempata a favor del vehículo que ya está cerca)
    private int minutosReposicion(Long vehiculoId, OperacionTransporte o) {
        OperacionTransporte antes = anterior(vehiculoId, o);
        if (antes == null || antes.getDestinoId().equals(o.getOrigenId())) return 0;
        Integer min = duracion.minutos(antes.getDestinoId(), o.getOrigenId(), "TRASLADO");
        return min == null ? 0 : min;
    }

    // Reglas de prioridad de la empresa (solo obligan a la asignación automática)
    private String advertencia(Proveedor p, OperacionTransporte o, Lugar origen) {
        if (o.getProveedorPrefId() != null && !o.getProveedorPrefId().equals(p.getProveedorId())) return "no es la empresa preferida";
        if (rechazos(o).contains(String.valueOf(p.getProveedorId()))) return "la empresa rechazó este servicio";
        if (p.getServicios() != null && !List.of(p.getServicios().split(",")).contains(o.getTipoServicio())) {
            return "la empresa no ofrece " + o.getTipoServicio();
        }
        if (p.getZonas() != null && origen != null && Arrays.stream(p.getZonas().split(","))
                .noneMatch(z -> z.trim().equalsIgnoreCase(origen.getCiudad()))) {
            return "la empresa no opera en " + origen.getCiudad();
        }
        return null;
    }

    // Reglas del conductor: null si la persona puede conducir la operación
    private String motivoConductor(Personal c, Proveedor p, OperacionTransporte o) {
        if ("GUIA".equals(c.getRol())) return "no es conductor (rol GUIA)";
        if (!"ACTIVO".equals(p.getEstado())) return "proveedor " + p.getEstado();
        if (!"DISPONIBLE".equals(c.getEstado())) return "conductor " + c.getEstado();
        LocalDate llegada = o.getHoraLlegada().toLocalDate();
        if (c.getVencLicencia() == null || c.getVencLicencia().isBefore(llegada)) {
            return "licencia vence el " + (c.getVencLicencia() == null ? "?" : c.getVencLicencia().format(FECHA));
        }
        if (o.getTipoVehiculoReq() != null && !licenciaPermite(c.getCatLicencia(), o.getTipoVehiculoReq())) {
            return "licencia " + c.getCatLicencia() + " no habilita " + o.getTipoVehiculoReq();
        }
        double horas = horasDelDia(c.getPersonalId(), o) + minutos(o) / 60.0;
        if (horas > MAX_HORAS_DIA) {
            return String.format(Locale.US, "excede %.0f h de conducción en el día (%.1f h)", MAX_HORAS_DIA, horas);
        }
        List<OperacionTransporte> cruces = operacionRepository.crucesConductor(c.getPersonalId(), idOMenosUno(o),
                o.getHoraSalida().minusMinutes(MARGEN_MIN), o.getHoraLlegada().plusMinutes(MARGEN_MIN));
        return cruces.isEmpty() ? null : "cruce de horario con " + cruces.get(0).getCodigoOperacion();
    }

    static boolean licenciaPermite(String categoria, String tipoVehiculo) {
        String minima = LICENCIA_MINIMA.get(tipoVehiculo);
        if (minima == null) return true;
        return categoria != null && CATEGORIAS.indexOf(categoria) >= CATEGORIAS.indexOf(minima);
    }

    // Horas ya programadas del conductor en el día de la operación (sin contarla)
    private double horasDelDia(Long conductorId, OperacionTransporte o) {
        LocalDateTime inicio = o.getHoraSalida().toLocalDate().atStartOfDay();
        return operacionRepository.delConductorEnElDia(conductorId, idOMenosUno(o), inicio, inicio.plusDays(1)).stream()
                .mapToDouble(x -> minutos(x) / 60.0).sum();
    }

    private int serviciosDelDia(Long vehiculoId, OperacionTransporte o) {
        LocalDateTime inicio = o.getHoraSalida().toLocalDate().atStartOfDay();
        return operacionRepository.delVehiculoEnElDia(vehiculoId, idOMenosUno(o), inicio, inicio.plusDays(1)).size();
    }

    // Una salida CONFIRMADA no puede quedarse sin el transporte que necesita
    private void validarSalidaConfirmada(Long salidaId) {
        if (salidaId == null) return;
        SalidaProgramada s = salidaRepository.findById(salidaId).orElse(null);
        if (s == null || !"CONFIRMADA".equals(s.getEstado())) return;
        String falta = capacidad.faltante(salidaId, s.getCuposMaximos() - s.getCuposDisponibles(), null);
        if (falta != null) {
            throw new BusinessException("La salida confirmada " + salidaId + " quedaría sin transporte suficiente: " + falta
                    + ". Para reemplazar un vehículo, asigna primero el nuevo y luego cancela el otro.");
        }
    }

    private void registrar(OperacionTransporte o, String observacion) {
        HistorialOperacion h = new HistorialOperacion();
        h.setOperacionId(o.getOperacionId());
        h.setEstadoNuevo(o.getEstado());
        h.setFechaCambio(LocalDateTime.now());
        h.setUsuario(SesionActual.usuario());
        h.setObservacion(observacion == null || observacion.isBlank() ? null : abreviar(observacion.trim()));
        historialRepository.save(h);
    }

    // ---------- Alcance por empresa ----------

    private void verificarAlcance(OperacionTransporte o) {
        Long propio = SesionActual.proveedorId();
        if (propio == null) return;
        Long prov = o.getVehiculoId() == null ? null
                : vehiculoRepository.findById(o.getVehiculoId()).map(Vehiculo::getProveedorId).orElse(null);
        if (!propio.equals(prov)) throw new AccesoDenegadoException("Este servicio no está asignado a tu empresa.");
    }

    private void soloKampu() {
        if (SesionActual.proveedorId() != null) {
            throw new AccesoDenegadoException("La disponibilidad de toda la flota solo la consulta Kampu Tours.");
        }
    }

    private static Long proveedorDe(OperacionTransporte o, Contexto ctx) {
        Vehiculo v = o.getVehiculoId() == null ? null : ctx.vehiculos().get(o.getVehiculoId());
        return v == null ? null : v.getProveedorId();
    }

    // ---------- Utilidades ----------

    private static Set<String> rechazos(OperacionTransporte o) {
        if (o.getProveedoresRechazo() == null || o.getProveedoresRechazo().isBlank()) return Set.of();
        return Arrays.stream(o.getProveedoresRechazo().split(",")).map(String::trim).collect(Collectors.toSet());
    }

    private static LocalDateTime salidaEfectiva(OperacionTransporte o) {
        return o.getHoraSalidaEstimada() != null ? o.getHoraSalidaEstimada() : o.getHoraSalida();
    }

    private static long minutos(OperacionTransporte o) {
        return Duration.between(o.getHoraSalida(), o.getHoraLlegada()).toMinutes();
    }

    private static long idOMenosUno(OperacionTransporte o) {
        return o.getOperacionId() == null ? -1L : o.getOperacionId();
    }

    private static String etiqueta(OperacionTransporte o) {
        return o.getCodigoOperacion() == null ? "Operación" : o.getCodigoOperacion();
    }

    private static String vacio(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String abreviar(String s) {
        return s.length() > 200 ? s.substring(0, 197) + "..." : s;
    }

    private static boolean esPropio(Proveedor p) {
        return p != null && Integer.valueOf(1).equals(p.getEsPropio());
    }

    private OperacionTransporte obtener(Long id) {
        return operacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operación de transporte no encontrada con Id: " + id));
    }

    private Map<Long, Proveedor> proveedores() {
        return proveedorRepository.findAll().stream().collect(Collectors.toMap(Proveedor::getProveedorId, Function.identity()));
    }

    // ---------- Armado del DTO (tablero FIDS) ----------

    private record Contexto(Map<Long, Lugar> lugares, Map<Long, Vehiculo> vehiculos, Map<Long, Personal> personal,
                            Map<Long, Proveedor> proveedores, Map<Long, List<HistorialOperacion>> historial,
                            List<OperacionTransporte> vigentes) {
    }

    private Contexto contexto() {
        return new Contexto(
                lugarRepository.findAll().stream().collect(Collectors.toMap(Lugar::getLugarId, Function.identity())),
                vehiculoRepository.findAll().stream().collect(Collectors.toMap(Vehiculo::getVehiculoId, Function.identity())),
                personalRepository.findAll().stream().collect(Collectors.toMap(Personal::getPersonalId, Function.identity())),
                proveedores(),
                historialRepository.findAllByOrderByHistorialIdAsc().stream().collect(Collectors.groupingBy(HistorialOperacion::getOperacionId)),
                operacionRepository.findAll().stream().filter(x -> x.getVehiculoId() != null
                        && !EstadoOperacion.de(x.getEstado()).esFinal()).toList());
    }

    // Control central de conflictos: otra operación vigente con el mismo vehículo o conductor que se cruza
    private String conflicto(OperacionTransporte o, Contexto ctx) {
        if (o.getVehiculoId() == null || EstadoOperacion.de(o.getEstado()).esFinal()) return null;
        for (OperacionTransporte x : ctx.vigentes()) {
            if (x.getOperacionId().equals(o.getOperacionId())) continue;
            boolean seCruzan = x.getHoraSalida().isBefore(o.getHoraLlegada()) && x.getHoraLlegada().isAfter(o.getHoraSalida());
            if (!seCruzan) continue;
            if (x.getVehiculoId().equals(o.getVehiculoId())) return "Vehículo también en " + x.getCodigoOperacion();
            if (x.getConductorId() != null && x.getConductorId().equals(o.getConductorId())) return "Conductor también en " + x.getCodigoOperacion();
        }
        return null;
    }

    private OperacionDTO dto(OperacionTransporte o, Contexto ctx) {
        Vehiculo v = o.getVehiculoId() == null ? null : ctx.vehiculos().get(o.getVehiculoId());
        Personal c = o.getConductorId() == null ? null : ctx.personal().get(o.getConductorId());
        Proveedor p = v != null ? ctx.proveedores().get(v.getProveedorId()) : null;
        Proveedor pref = o.getProveedorPrefId() == null ? null : ctx.proveedores().get(o.getProveedorPrefId());
        LocalDateTime realSalida = null, realLlegada = null;
        for (HistorialOperacion h : ctx.historial().getOrDefault(o.getOperacionId(), List.of())) {
            if ("EN_RUTA".equals(h.getEstadoNuevo()) && realSalida == null) realSalida = h.getFechaCambio();
            if ("FINALIZADO".equals(h.getEstadoNuevo())) realLlegada = h.getFechaCambio();
        }
        EstadoOperacion e = EstadoOperacion.de(o.getEstado());
        Lugar origen = ctx.lugares().get(o.getOrigenId()), destino = ctx.lugares().get(o.getDestinoId());
        return new OperacionDTO(o.getOperacionId(), o.getCodigoOperacion(), o.getSalidaId(), o.getOrigenId(), o.getDestinoId(),
                o.getVehiculoId(), o.getConductorId(), o.getPaxProgramados(), o.getHoraSalida(), o.getHoraLlegada(),
                o.getEstado(), o.getObservaciones() == null ? "" : o.getObservaciones(),
                origen == null ? "" : origen.getNombre(), destino == null ? "" : destino.getNombre(),
                p == null ? null : p.getProveedorId(), p == null ? null : p.getNombreComercial(), esPropio(p),
                v == null ? null : v.getPlaca(), c == null ? null : c.getNombreCompleto(), e.nombre(), e.orden(),
                realSalida, realLlegada,
                realSalida == null ? null : (int) Duration.between(o.getHoraSalida(), realSalida).toMinutes(),
                realLlegada == null ? null : (int) Duration.between(o.getHoraLlegada(), realLlegada).toMinutes(),
                o.getTipoServicio(), o.getRuta() == null ? "" : o.getRuta(), o.getTipoVehiculoReq(),
                o.getProveedorPrefId(), pref == null ? null : pref.getNombreComercial(),
                v == null ? null : v.getTipoVehiculo(), v == null ? null : v.getCapacidadPax(),
                origen == null ? "" : origen.getCiudad(), destino == null ? "" : destino.getCiudad(),
                o.getHoraSalidaEstimada(),
                o.getHoraSalidaEstimada() == null ? null : (int) Duration.between(o.getHoraSalida(), o.getHoraSalidaEstimada()).toMinutes(),
                o.getConfirmacion(), o.getMotivoRechazo(), conflicto(o, ctx), e.siguientesOrdenados());
    }
}
