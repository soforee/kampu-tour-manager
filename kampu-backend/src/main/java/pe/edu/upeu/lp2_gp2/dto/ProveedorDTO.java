package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Proveedor;

import java.time.LocalDate;
import java.util.List;

public record ProveedorDTO(
        Long proveedorId,
        String nombreComercial,
        String ruc,
        String telefono,
        boolean esPropio,
        String estado,
        // Calculados: flota y personal del proveedor
        int vehiculos,
        int capacidadOperativa,
        int conductores,
        int guias,
        // Afiliación
        String direccion,
        String email,
        String contacto,
        List<String> servicios,
        String zonas,
        LocalDate fechaAfiliacion,
        // Disponibilidad operativa: vehículos libres ahora y servicios activos hoy
        int vehiculosDisponibles,
        int serviciosHoy,
        int serviciosPorConfirmar
) {
    public static ProveedorDTO desde(Proveedor p, int vehiculos, int capacidad, int conductores, int guias,
                                     int disponibles, int serviciosHoy, int porConfirmar) {
        return new ProveedorDTO(p.getProveedorId(), p.getNombreComercial(), p.getRuc(), p.getTelefono() == null ? "" : p.getTelefono(),
                Integer.valueOf(1).equals(p.getEsPropio()), p.getEstado(), vehiculos, capacidad, conductores, guias,
                texto(p.getDireccion()), texto(p.getEmail()), texto(p.getContacto()),
                p.getServicios() == null ? List.of() : List.of(p.getServicios().split(",")),
                texto(p.getZonas()), p.getFechaAfiliacion(), disponibles, serviciosHoy, porConfirmar);
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}
