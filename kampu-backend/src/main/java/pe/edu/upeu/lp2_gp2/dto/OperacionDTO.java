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
        Integer minRetrasoLlegada
) {
}
