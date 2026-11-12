package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Proveedor;

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
        int guias
) {
    public static ProveedorDTO desde(Proveedor p, int vehiculos, int capacidad, int conductores, int guias) {
        return new ProveedorDTO(p.getProveedorId(), p.getNombreComercial(), p.getRuc(), p.getTelefono() == null ? "" : p.getTelefono(),
                Integer.valueOf(1).equals(p.getEsPropio()), p.getEstado(), vehiculos, capacidad, conductores, guias);
    }
}
