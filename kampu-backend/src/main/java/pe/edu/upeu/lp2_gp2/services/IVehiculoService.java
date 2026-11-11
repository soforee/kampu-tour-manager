package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.VehiculoDTO;
import pe.edu.upeu.lp2_gp2.dto.VehiculoRequestDTO;

import java.util.List;

public interface IVehiculoService {
    List<VehiculoDTO> listarTodo();
    VehiculoDTO buscarPorId(Long id);
    VehiculoDTO crear(VehiculoRequestDTO dto);
    VehiculoDTO actualizar(Long id, VehiculoRequestDTO dto);
    void eliminar(Long id);
}
