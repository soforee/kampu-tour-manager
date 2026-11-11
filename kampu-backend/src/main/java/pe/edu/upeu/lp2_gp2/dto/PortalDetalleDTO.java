package pe.edu.upeu.lp2_gp2.dto;

import java.util.List;

// Detalle de un paquete propio del cliente (solo lectura)
public record PortalDetalleDTO(
        PortalPaqueteDTO paquete,
        String descripcion,
        String guia,
        List<PortalPasajeroDTO> pasajeros,
        List<PagoDTO> pagos
) {
    public record PortalPasajeroDTO(String nombreCompleto, String tipoDocumento, String numeroDocumento, String nacionalidad) {
    }
}
