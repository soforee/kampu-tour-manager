package pe.edu.upeu.lp2_gp2.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.EstimacionDuracionDTO;
import pe.edu.upeu.lp2_gp2.entity.Lugar;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.repository.LugarRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * Duración de una ruta con datos reales:
 *  1. OSRM (https://project-osrm.org): tiempo de manejo por la red vial de OpenStreetMap
 *     entre las coordenadas del origen y del destino, más un margen de embarque.
 *  2. Si OSRM no responde (sin internet) o está desactivado: estimación con la distancia real en
 *     línea recta entre las coordenadas (haversine), calibrada con tiempos de viaje reales del Perú.
 * v4: el resultado se guarda directamente en OPERACIONES_TRANSPORTE.hora_llegada.
 * Configuración: kampu.rutas.osrm-url y kampu.rutas.osrm-habilitado (application.yaml).
 */
@Service
public class DuracionRutaService {
    private final LugarRepository lugarRepository;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http;
    private final String osrmUrl;
    private final boolean osrmHabilitado;
    // La asignación consulta muchas veces los mismos tramos: se guardan en memoria (origen-destino-tipo)
    private final java.util.Map<String, EstimacionDuracionDTO> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public DuracionRutaService(LugarRepository lugarRepository,
                               @Value("${kampu.rutas.osrm-url:https://router.project-osrm.org}") String osrmUrl,
                               @Value("${kampu.rutas.osrm-habilitado:true}") boolean osrmHabilitado) {
        this.lugarRepository = lugarRepository;
        this.osrmUrl = osrmUrl.endsWith("/") ? osrmUrl.substring(0, osrmUrl.length() - 1) : osrmUrl;
        this.osrmHabilitado = osrmHabilitado;
        // ProxySelector.getDefault(): respeta https.proxyHost/https.proxyPort si la red lo necesita
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
                .proxy(ProxySelector.getDefault()).build();
    }

    /** Duración en minutos, o null si ningún lugar tiene coordenadas (entonces se debe indicar a mano). */
    @Transactional(readOnly = true)
    public EstimacionDuracionDTO estimar(Long origenId, Long destinoId, String tipoServicio) {
        Lugar origen = lugarRepository.findById(origenId)
                .orElseThrow(() -> new BusinessException("El origen seleccionado no existe."));
        Lugar destino = lugarRepository.findById(destinoId)
                .orElseThrow(() -> new BusinessException("El destino seleccionado no existe."));
        if (origenId.equals(destinoId)) throw new BusinessException("El origen y el destino deben ser distintos.");
        if (!tieneCoordenadas(origen) || !tieneCoordenadas(destino)) {
            String falta = !tieneCoordenadas(origen) ? origen.getNombre() : destino.getNombre();
            return new EstimacionDuracionDTO(null, null, "SIN_COORDENADAS",
                    "Registra la latitud y longitud de " + falta + " (Logística → Lugares) o indica la duración a mano.");
        }
        String tipo = tipoServicio == null ? "TRASLADO" : tipoServicio;

        EstimacionDuracionDTO real = osrm(origen, destino, tipo);
        if (real != null) return real;

        double recta = haversineKm(origen, destino);
        double velocidad = Math.min(VEL_MAXIMA, VEL_BASE + VEL_POR_KM * recta);
        int minutos = (int) Math.max(5, Math.ceil((recta * FACTOR_VIAL / velocidad * 60 + margen(tipo)) / 5) * 5);
        BigDecimal km = BigDecimal.valueOf(recta * FACTOR_VIAL).setScale(1, RoundingMode.HALF_UP);
        return new EstimacionDuracionDTO(minutos, km, "ESTIMADO",
                "Estimación con las coordenadas reales de los lugares: sin conexión con el servicio de rutas OSRM.");
    }

    // Estimación sin internet: carretera = línea recta x 1.3; velocidad media 18 km/h + 0.3 x km (máx. 80)
    private static final double FACTOR_VIAL = 1.3;
    private static final double VEL_BASE = 18;
    private static final double VEL_POR_KM = 0.3;
    private static final double VEL_MAXIMA = 80;

    private double haversineKm(Lugar a, Lugar b) {
        double lat1 = Math.toRadians(a.getLatitud().doubleValue()), lat2 = Math.toRadians(b.getLatitud().doubleValue());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.getLongitud().doubleValue() - a.getLongitud().doubleValue());
        double h = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6371 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }

    /** Minutos del trayecto, o null si faltan coordenadas. */
    public Integer minutos(Long origenId, Long destinoId, String tipoServicio) {
        if (origenId == null || destinoId == null || origenId.equals(destinoId)) return null;
        String clave = origenId + "-" + destinoId + "-" + (tipoServicio == null ? "TRASLADO" : tipoServicio);
        EstimacionDuracionDTO e = cache.get(clave);
        if (e == null) {
            e = estimar(origenId, destinoId, tipoServicio);
            if (e.minutos() != null) cache.put(clave, e);
        }
        return e.minutos();
    }

    /** Al cambiar las coordenadas de un lugar se recalculan sus tramos */
    public void olvidar() {
        cache.clear();
    }

    // Embarque y desembarque que OSRM no considera
    private int margen(String tipo) {
        return "INTERPROVINCIAL".equals(tipo) ? 10 : 5;
    }

    private EstimacionDuracionDTO osrm(Lugar o, Lugar d, String tipo) {
        if (!osrmHabilitado) return null;
        String url = String.format(Locale.US, "%s/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=false",
                osrmUrl, o.getLongitud(), o.getLatitud(), d.getLongitud(), d.getLatitud());
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "kampu-tour-manager").GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            JsonNode raiz = json.readTree(resp.body());
            if (!"Ok".equals(raiz.path("code").asText()) || raiz.path("routes").isEmpty()) return null;
            JsonNode ruta = raiz.path("routes").get(0);
            double segundos = ruta.path("duration").asDouble();
            double metros = ruta.path("distance").asDouble();
            int minutos = (int) Math.max(5, Math.ceil((segundos / 60 + margen(tipo)) / 5) * 5);
            BigDecimal km = BigDecimal.valueOf(metros / 1000).setScale(1, RoundingMode.HALF_UP);
            return new EstimacionDuracionDTO(minutos, km, "OSRM",
                    "Tiempo de manejo por carretera (OpenStreetMap / OSRM) + " + margen(tipo) + " min de embarque.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            // Sin internet, servicio caído o respuesta inesperada: se usa la estimación por coordenadas
            return null;
        }
    }

    private boolean tieneCoordenadas(Lugar l) {
        return l.getLatitud() != null && l.getLongitud() != null;
    }
}
