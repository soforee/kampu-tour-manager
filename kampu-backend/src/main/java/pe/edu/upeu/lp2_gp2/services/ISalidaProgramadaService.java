package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.ManifiestoDTO;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaDTO;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaRequestDTO;

import java.util.List;

public interface ISalidaProgramadaService {
    List<SalidaProgramadaDTO> listarTodo();
    SalidaProgramadaDTO buscarPorId(Long id);
    SalidaProgramadaDTO crear(SalidaProgramadaRequestDTO dto);
    SalidaProgramadaDTO actualizar(Long id, SalidaProgramadaRequestDTO dto);
    // Operador / Ventas: marcar la salida en ejecución (CONFIRMADA) o terminada (COMPLETADA)
    SalidaProgramadaDTO actualizarEjecucion(Long id, String estado);
    // Lista de pasajeros de la salida (reservas no anuladas)
    ManifiestoDTO manifiesto(Long id, boolean conMontos);
    void eliminar(Long id);
}
