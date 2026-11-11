package pe.edu.upeu.lp2_gp2.dto;

import pe.edu.upeu.lp2_gp2.entity.Vehiculo;

import java.time.LocalDate;

public record VehiculoDTO(
        Long vehiculoId,
        Long proveedorId,
        String placa,
        String tipoVehiculo,
        String marcaModelo,
        Integer capacidadPax,
        LocalDate proxMantenimiento,
        String estado
) {
    public static VehiculoDTO desde(Vehiculo v) {
        return new VehiculoDTO(v.getVehiculoId(), v.getProveedorId(), v.getPlaca(), v.getTipoVehiculo(),
                v.getMarcaModelo() == null ? "" : v.getMarcaModelo(), v.getCapacidadPax(), v.getProxMantenimiento(), v.getEstado());
    }
}
