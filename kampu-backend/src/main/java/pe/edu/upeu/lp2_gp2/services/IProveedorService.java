package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.ProveedorDTO;
import pe.edu.upeu.lp2_gp2.dto.ProveedorRequestDTO;

import java.util.List;

public interface IProveedorService {
    List<ProveedorDTO> listarTodo();
    ProveedorDTO buscarPorId(Long id);
    ProveedorDTO crear(ProveedorRequestDTO dto);
    ProveedorDTO actualizar(Long id, ProveedorRequestDTO dto);
    void eliminar(Long id);
}
