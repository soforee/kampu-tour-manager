package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.*;

import java.util.List;

public interface IOperacionTransporteService {
    List<OperacionDTO> listar(Long salidaId);
    OperacionDTO buscarPorId(Long id);
    OperacionDTO crear(OperacionRequestDTO dto);
    OperacionDTO actualizar(Long id, OperacionRequestDTO dto);
    OperacionDTO cambiarEstado(Long id, CambioEstadoRequestDTO dto);
    OperacionDTO asignarAutomatico(Long id);
    OperacionDTO asignar(Long id, AsignacionRequestDTO dto);
    List<HistorialOperacionDTO> historial(Long id);
    List<RecursoCandidatoDTO> candidatos(Long id);
    List<EstadoOperacionDTO> estados();
    EstimacionDuracionDTO estimarDuracion(Long origenId, Long destinoId, String tipoServicio);
    AsignacionMasivaDTO asignarPendientes(java.time.LocalDate fecha);
    OperacionDTO responder(Long id, RespuestaServicioRequestDTO dto);
}
