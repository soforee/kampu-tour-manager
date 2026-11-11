package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record OperacionRequestDTO(
        // Salida/tour al que atiende (opcional: traslado suelto)
        Long salidaId,
        @NotNull(message = "Selecciona el origen.")
        Long origenId,
        @NotNull(message = "Selecciona el destino.")
        Long destinoId,
        // Opcionales (van juntos): sin ellos la operación queda PROGRAMADA
        Long vehiculoId,
        Long conductorId,
        @NotNull(message = "Indica los pasajeros programados.")
        @Min(value = 1, message = "Los pasajeros deben estar entre 1 y 999.")
        @Max(value = 999, message = "Los pasajeros deben estar entre 1 y 999.")
        Integer paxProgramados,
        @NotNull(message = "La hora de salida es obligatoria.")
        LocalDateTime horaProgramadaSalida,
        // Vacía: el backend la calcula (OSRM / coordenadas de los lugares)
        LocalDateTime horaEstimadaLlegada,
        @Size(max = 200, message = "Las observaciones admiten máximo 200 caracteres.")
        String observaciones
) {
}
