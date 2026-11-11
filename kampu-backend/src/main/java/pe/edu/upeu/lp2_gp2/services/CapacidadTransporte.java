package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Component;
import pe.edu.upeu.lp2_gp2.entity.Lugar;
import pe.edu.upeu.lp2_gp2.entity.OperacionTransporte;
import pe.edu.upeu.lp2_gp2.entity.Vehiculo;
import pe.edu.upeu.lp2_gp2.repository.LugarRepository;
import pe.edu.upeu.lp2_gp2.repository.OperacionTransporteRepository;
import pe.edu.upeu.lp2_gp2.repository.VehiculoRepository;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ¿Alcanza el transporte asignado a una salida para sus pasajeros?
 * Un "tramo" son las operaciones vigentes de la salida con el mismo origen, destino y hora de
 * salida; la suma de la capacidad de sus vehículos debe cubrir a los pasajeros vendidos.
 */
@Component
public class CapacidadTransporte {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OperacionTransporteRepository operacionRepository;
    private final VehiculoRepository vehiculoRepository;
    private final LugarRepository lugarRepository;

    public CapacidadTransporte(OperacionTransporteRepository operacionRepository, VehiculoRepository vehiculoRepository,
                               LugarRepository lugarRepository) {
        this.operacionRepository = operacionRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.lugarRepository = lugarRepository;
    }

    /**
     * null si el transporte alcanza; si no, el primer tramo que no alcanza.
     * soloTramoDe: si no es null, solo revisa el tramo de esa operación.
     */
    public String faltante(Long salidaId, int pasajeros, OperacionTransporte soloTramoDe) {
        if (salidaId == null || pasajeros <= 0) return null;
        Map<String, List<OperacionTransporte>> tramos = new TreeMap<>();
        for (OperacionTransporte o : operacionRepository.findBySalidaIdOrderByHoraSalidaAscOperacionIdAsc(salidaId)) {
            if ("CANCELADO".equals(o.getEstado())) continue;
            if (soloTramoDe != null && !mismoTramo(o, soloTramoDe)) continue;
            tramos.computeIfAbsent(o.getHoraSalida() + "|" + o.getOrigenId() + "|" + o.getDestinoId(), k -> new ArrayList<>()).add(o);
        }
        for (List<OperacionTransporte> tramo : tramos.values()) {
            int capacidad = 0;
            for (OperacionTransporte o : tramo) {
                if (o.getVehiculoId() != null) {
                    capacidad += vehiculoRepository.findById(o.getVehiculoId()).map(Vehiculo::getCapacidadPax).orElse(0);
                }
            }
            if (capacidad < pasajeros) {
                OperacionTransporte o = tramo.get(0);
                return "tramo " + nombre(o.getOrigenId()) + " -> " + nombre(o.getDestinoId())
                        + " (" + o.getHoraSalida().format(FORMATO) + "): capacidad " + capacidad + " < " + pasajeros + " pasajeros";
            }
        }
        return null;
    }

    private boolean mismoTramo(OperacionTransporte a, OperacionTransporte b) {
        return a.getOrigenId().equals(b.getOrigenId()) && a.getDestinoId().equals(b.getDestinoId())
                && a.getHoraSalida().equals(b.getHoraSalida());
    }

    private String nombre(Long lugarId) {
        return lugarRepository.findById(lugarId).map(Lugar::getNombre).orElse("lugar " + lugarId);
    }
}
