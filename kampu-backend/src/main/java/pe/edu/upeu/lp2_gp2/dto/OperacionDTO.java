package pe.edu.upeu.lp2_gp2.dto;

import java.time.LocalDateTime;

// Operación de transporte con los nombres para el tablero (FIDS)
public record OperacionDTO(
        Long operacionId,
        String codigoOperacion,
        Long salidaId,
        Long origenId,
        Long destinoId,
        Long vehiculoId,
        Long conductorId,
        Integer paxProgramados,
        LocalDateTime horaProgramadaSalida,
        LocalDateTime horaEstimadaLlegada,
        String estadoActual,
        String observaciones,
        String origen,
        String destino,
        Long proveedorId,
        String proveedor,
        boolean proveedorPropio,
        String placa,
        String conductor,
        String estadoNombre,
        Integer ordenEstado,
        // Del historial: cuándo salió y llegó realmente y los minutos de retraso
        LocalDateTime horaRealSalida,
        LocalDateTime horaRealLlegada,
        Integer minRetrasoSalida,
        Integer minRetrasoLlegada,
        // Programación del servicio
        String tipoServicio,
        String ruta,
        String tipoVehiculoReq,
        Long proveedorPrefId,
        String proveedorPref,
        String tipoVehiculo,
        Integer capacidadVehiculo,
        String ciudadOrigen,
        String ciudadDestino,
        // Retraso: nueva hora de salida y minutos de demora frente a lo programado
        LocalDateTime horaSalidaEstimada,
        Integer minRetrasoEstimado,
        // Respuesta de la empresa: PENDIENTE, ACEPTADO, RECHAZADO
        String confirmacion,
        String motivoRechazo,
        // Control central: cruce de horario detectado con otra operación del mismo vehículo o conductor
        String conflicto,
        java.util.List<String> siguientes
) {
}
