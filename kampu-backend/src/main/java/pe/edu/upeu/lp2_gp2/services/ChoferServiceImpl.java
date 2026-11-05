package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.ChoferDTO;
import pe.edu.upeu.lp2_gp2.dto.ChoferRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.Reglas;
import pe.edu.upeu.lp2_gp2.entity.Chofer;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.DuplicateResourceException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.ChoferRepository;
import pe.edu.upeu.lp2_gp2.repository.SalidaProgramadaRepository;
import pe.edu.upeu.lp2_gp2.repository.TransporteRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ChoferServiceImpl implements IChoferService {
    private final ChoferRepository choferRepository;
    private final TransporteRepository transporteRepository;
    private final SalidaProgramadaRepository salidaRepository;

    public ChoferServiceImpl(ChoferRepository choferRepository, TransporteRepository transporteRepository,
                             SalidaProgramadaRepository salidaRepository) {
        this.choferRepository = choferRepository;
        this.transporteRepository = transporteRepository;
        this.salidaRepository = salidaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoferDTO> listarTodo() {
        return choferRepository.findAllByOrderByChoferIdAsc().stream().map(ChoferDTO::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChoferDTO buscarPorId(Long id) {
        return ChoferDTO.desde(obtener(id));
    }

    @Override
    @Transactional
    public ChoferDTO crear(ChoferRequestDTO dto) {
        Chofer c = new Chofer();
        copiar(dto, c, null);
        return ChoferDTO.desde(choferRepository.save(c));
    }

    @Override
    @Transactional
    public ChoferDTO actualizar(Long id, ChoferRequestDTO dto) {
        Chofer c = obtener(id);
        copiar(dto, c, id);
        return ChoferDTO.desde(choferRepository.save(c));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Chofer c = obtener(id);
        if (salidaRepository.existsByChoferId(id)) {
            throw new BusinessException("El chofer está asignado a salidas programadas. Puedes marcarlo como INACTIVO.");
        }
        choferRepository.delete(c);
    }

    private Chofer obtener(Long id) {
        return choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer no encontrado con Id: " + id));
    }

    private void copiar(ChoferRequestDTO dto, Chofer c, Long idActual) {
        if (!Reglas.telefonoValido(dto.telefono())) throw new BusinessException(Reglas.MSG_TELEFONO);
        String licencia = dto.numeroLicencia().trim().toUpperCase();
        Optional<Chofer> mismo = choferRepository.findByNumeroLicenciaIgnoreCase(licencia);
        if (mismo.isPresent() && !mismo.get().getChoferId().equals(idActual)) {
            throw new DuplicateResourceException("Ya existe un chofer con la licencia " + licencia + ".");
        }
        if (dto.transporteId() != null && !transporteRepository.existsById(dto.transporteId())) {
            throw new BusinessException("El vehículo seleccionado no existe.");
        }
        c.setNombreCompleto(dto.nombreCompleto().trim());
        c.setNumeroLicencia(licencia);
        c.setCategoriaLicencia(dto.categoriaLicencia());
        c.setTelefono(dto.telefono() == null || dto.telefono().isBlank() ? null : dto.telefono().trim());
        c.setTransporteId(dto.transporteId());
        c.setEstado(dto.estado());
    }
}
