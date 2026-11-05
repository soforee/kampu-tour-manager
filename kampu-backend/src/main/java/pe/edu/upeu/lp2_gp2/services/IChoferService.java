package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.ChoferDTO;
import pe.edu.upeu.lp2_gp2.dto.ChoferRequestDTO;

import java.util.List;

public interface IChoferService {
    List<ChoferDTO> listarTodo();
    ChoferDTO buscarPorId(Long id);
    ChoferDTO crear(ChoferRequestDTO dto);
    ChoferDTO actualizar(Long id, ChoferRequestDTO dto);
    void eliminar(Long id);
}
