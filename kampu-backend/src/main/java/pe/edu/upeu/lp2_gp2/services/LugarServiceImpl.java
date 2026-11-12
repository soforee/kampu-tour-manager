package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.LugarDTO;
import pe.edu.upeu.lp2_gp2.dto.LugarRequestDTO;
import pe.edu.upeu.lp2_gp2.entity.Lugar;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.DuplicateResourceException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.LugarRepository;
import pe.edu.upeu.lp2_gp2.repository.OperacionTransporteRepository;

import java.util.List;
import java.util.Optional;

@Service
public class LugarServiceImpl implements ILugarService {
    private final LugarRepository lugarRepository;
    private final OperacionTransporteRepository operacionRepository;

    public LugarServiceImpl(LugarRepository lugarRepository,
                            OperacionTransporteRepository operacionRepository) {
        this.lugarRepository = lugarRepository;
        this.operacionRepository = operacionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LugarDTO> listarTodo() {
        return lugarRepository.findAllByOrderByCiudadAscNombreAsc().stream().map(LugarDTO::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LugarDTO buscarPorId(Long id) {
        return LugarDTO.desde(obtener(id));
    }

    @Override
    @Transactional
    public LugarDTO crear(LugarRequestDTO dto) {
        Lugar l = new Lugar();
        copiar(dto, l, null);
        return LugarDTO.desde(lugarRepository.saveAndFlush(l));
    }

    @Override
    @Transactional
    public LugarDTO actualizar(Long id, LugarRequestDTO dto) {
        Lugar l = obtener(id);
        copiar(dto, l, id);
        return LugarDTO.desde(lugarRepository.saveAndFlush(l));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Lugar l = obtener(id);
        if (operacionRepository.existsByOrigenIdOrDestinoId(id, id)) {
            throw new BusinessException("El lugar se usa en operaciones de transporte.");
        }
        lugarRepository.delete(l);
    }

    private Lugar obtener(Long id) {
        return lugarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lugar no encontrado con Id: " + id));
    }

    private void copiar(LugarRequestDTO dto, Lugar l, Long id) {
        String nombre = dto.nombre().trim();
        String ciudad = dto.ciudad().trim();
        Optional<Lugar> mismo = lugarRepository.findByNombreIgnoreCaseAndCiudadIgnoreCase(nombre, ciudad);
        if (mismo.isPresent() && !mismo.get().getLugarId().equals(id)) {
            throw new DuplicateResourceException("Ya existe el lugar " + nombre + " en " + ciudad + ".");
        }
        if ((dto.latitud() == null) != (dto.longitud() == null)) {
            throw new BusinessException("Indica la latitud y la longitud juntas (o deja ambas vacías).");
        }
        l.setNombre(nombre);
        l.setTipoLugar(dto.tipoLugar());
        l.setCiudad(ciudad);
        l.setLatitud(dto.latitud());
        l.setLongitud(dto.longitud());
    }
}
