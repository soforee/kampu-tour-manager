package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.ClienteDTO;
import pe.edu.upeu.lp2_gp2.dto.ClienteRequestDTO;

import java.util.List;

public interface IClienteService {
    List<ClienteDTO> listarTodo();
    List<ClienteDTO> buscar(String texto);
    ClienteDTO buscarPorId(Long id);
    ClienteDTO crear(ClienteRequestDTO dto);
    ClienteDTO actualizar(Long id, ClienteRequestDTO dto);
    void eliminar(Long id);
}
