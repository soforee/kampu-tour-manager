package pe.edu.upeu.lp2_gp2.services;

import pe.edu.upeu.lp2_gp2.dto.PersonalDTO;
import pe.edu.upeu.lp2_gp2.dto.PersonalRequestDTO;

import java.util.List;

public interface IPersonalService {
    List<PersonalDTO> listarTodo();
    PersonalDTO buscarPorId(Long id);
    PersonalDTO crear(PersonalRequestDTO dto);
    PersonalDTO actualizar(Long id, PersonalRequestDTO dto);
    void eliminar(Long id);
}
