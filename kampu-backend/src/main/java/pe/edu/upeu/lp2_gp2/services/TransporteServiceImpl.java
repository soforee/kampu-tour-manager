package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.TransporteDTO;
import pe.edu.upeu.lp2_gp2.dto.TransporteRequestDTO;
import pe.edu.upeu.lp2_gp2.entity.Transporte;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.DuplicateResourceException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.SalidaProgramadaRepository;
import pe.edu.upeu.lp2_gp2.repository.TransporteRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TransporteServiceImpl implements ITransporteService {
    private final TransporteRepository transporteRepository;
    private final SalidaProgramadaRepository salidaRepository;

    public TransporteServiceImpl(TransporteRepository transporteRepository, SalidaProgramadaRepository salidaRepository) {
        this.transporteRepository = transporteRepository;
        this.salidaRepository = salidaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransporteDTO> listarTodo() {
        return transporteRepository.findAllByOrderByTransporteIdAsc().stream().map(TransporteDTO::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransporteDTO buscarPorId(Long id) {
        return TransporteDTO.desde(obtener(id));
    }

    @Override
    @Transactional
    public TransporteDTO crear(TransporteRequestDTO dto) {
        Transporte t = new Transporte();
        copiar(dto, t, null);
        return TransporteDTO.desde(transporteRepository.save(t));
    }

    @Override
    @Transactional
    public TransporteDTO actualizar(Long id, TransporteRequestDTO dto) {
        Transporte t = obtener(id);
        copiar(dto, t, id);
        return TransporteDTO.desde(transporteRepository.save(t));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Transporte t = obtener(id);
        if (salidaRepository.existsByTransporteId(id)) throw new BusinessException("El transporte está asignado a salidas programadas.");
        transporteRepository.delete(t);
    }

    private Transporte obtener(Long id) {
        return transporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transporte no encontrado con Id: " + id));
    }

    private void copiar(TransporteRequestDTO dto, Transporte t, Long id) {
        String placa = dto.placa().trim().toUpperCase();
        Optional<Transporte> existente = transporteRepository.findByPlacaIgnoreCase(placa);
        if (existente.isPresent() && !existente.get().getTransporteId().equals(id)) {
            throw new DuplicateResourceException("Ya existe un transporte con esa placa.");
        }
        t.setPlaca(placa);
        t.setMarcaModelo(dto.marcaModelo() == null || dto.marcaModelo().isBlank() ? null : dto.marcaModelo().trim());
        t.setCapacidad(dto.capacidad());
    }
}
