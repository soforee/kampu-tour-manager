package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.LugarDTO;
import pe.edu.upeu.lp2_gp2.dto.LugarRequestDTO;

import java.util.List;

public interface ILugarService {
    List<LugarDTO> listarTodo();
    LugarDTO buscarPorId(Long id);
    LugarDTO crear(LugarRequestDTO dto);
    LugarDTO actualizar(Long id, LugarRequestDTO dto);
    void eliminar(Long id);
}
