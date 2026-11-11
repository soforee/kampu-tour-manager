package pe.edu.upeu.lp2_gp2.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record VehiculoRequestDTO(
        @NotNull(message = "Selecciona el proveedor (Kampu Tours si es flota propia).")
        Long proveedorId,
        @NotBlank(message = "La placa es obligatoria.")
        @Pattern(regexp = Reglas.PLACA, message = "La placa solo admite letras, números y guion (3 a 10 caracteres).")
        String placa,
        @NotNull(message = "El tipo de vehículo es obligatorio.")
        @Pattern(regexp = "AUTO|CAMIONETA_4X4|VAN|MINIBUS|BUS", message = "Tipo de vehículo no válido.")
        String tipoVehiculo,
        @Size(max = 100, message = "La marca/modelo admite máximo 100 caracteres.")
        @Pattern(regexp = Reglas.TEXTO, message = "La marca/modelo contiene caracteres no permitidos.")
        String marcaModelo,
        @NotNull(message = "La capacidad es obligatoria.")
        @Min(value = 1, message = "La capacidad debe estar entre 1 y 999.")
        @Max(value = 999, message = "La capacidad debe estar entre 1 y 999.")
        Integer capacidadPax,
        LocalDate proxMantenimiento,
        @NotNull(message = "El estado es obligatorio.")
        @Pattern(regexp = "OPERATIVO|MANTENIMIENTO|INACTIVO", message = "El estado debe ser OPERATIVO, MANTENIMIENTO o INACTIVO.")
        String estado
) {
}
