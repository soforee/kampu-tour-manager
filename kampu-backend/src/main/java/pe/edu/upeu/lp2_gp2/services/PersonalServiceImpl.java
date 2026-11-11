package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.PersonalDTO;
import pe.edu.upeu.lp2_gp2.dto.PersonalRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.Reglas;
import pe.edu.upeu.lp2_gp2.entity.Personal;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.DuplicateResourceException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.OperacionTransporteRepository;
import pe.edu.upeu.lp2_gp2.repository.PersonalRepository;
import pe.edu.upeu.lp2_gp2.repository.ProveedorRepository;
import pe.edu.upeu.lp2_gp2.repository.SalidaProgramadaRepository;

import pe.edu.upeu.lp2_gp2.seguridad.SesionActual;

import java.util.List;
import java.util.Optional;

// Guías y conductores (un mismo registro puede ser AMBOS)
@Service
public class PersonalServiceImpl implements IPersonalService {
    private final PersonalRepository personalRepository;
    private final ProveedorRepository proveedorRepository;
    private final SalidaProgramadaRepository salidaRepository;
    private final OperacionTransporteRepository operacionRepository;

    public PersonalServiceImpl(PersonalRepository personalRepository, ProveedorRepository proveedorRepository,
                               SalidaProgramadaRepository salidaRepository, OperacionTransporteRepository operacionRepository) {
        this.personalRepository = personalRepository;
        this.proveedorRepository = proveedorRepository;
        this.salidaRepository = salidaRepository;
        this.operacionRepository = operacionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalDTO> listarTodo() {
        Long propio = SesionActual.proveedorId();   // una empresa proveedora solo ve su personal
        return personalRepository.findAllByOrderByPersonalIdAsc().stream()
                .filter(p -> propio == null || propio.equals(p.getProveedorId()))
                .map(PersonalDTO::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalDTO buscarPorId(Long id) {
        Personal p = obtener(id);
        SesionActual.verificarProveedor(p.getProveedorId());
        return PersonalDTO.desde(p);
    }

    @Override
    @Transactional
    public PersonalDTO crear(PersonalRequestDTO dto) {
        Personal p = new Personal();
        copiar(dto, p, null);
        return PersonalDTO.desde(personalRepository.saveAndFlush(p));
    }

    @Override
    @Transactional
    public PersonalDTO actualizar(Long id, PersonalRequestDTO dto) {
        Personal p = obtener(id);
        SesionActual.verificarProveedor(p.getProveedorId());
        if ("CONDUCTOR".equals(dto.rol()) && salidaRepository.existsByGuiaId(id)) {
            throw new BusinessException("Es guía de salidas programadas: su rol debe incluir GUIA (GUIA o AMBOS).");
        }
        if ("GUIA".equals(dto.rol()) && operacionRepository.existsByConductorId(id)) {
            throw new BusinessException("Conduce operaciones de transporte: su rol debe incluir CONDUCTOR (CONDUCTOR o AMBOS).");
        }
        copiar(dto, p, id);
        return PersonalDTO.desde(personalRepository.saveAndFlush(p));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Personal p = obtener(id);
        SesionActual.verificarProveedor(p.getProveedorId());
        if (salidaRepository.existsByGuiaId(id) || operacionRepository.existsByConductorId(id)) {
            throw new BusinessException("Está asignado a salidas u operaciones. Puedes marcarlo como INACTIVO.");
        }
        personalRepository.delete(p);
    }

    private Personal obtener(Long id) {
        return personalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado con Id: " + id));
    }

    private void copiar(PersonalRequestDTO dto, Personal p, Long id) {
        SesionActual.verificarProveedor(dto.proveedorId());
        if (!proveedorRepository.existsById(dto.proveedorId())) throw new BusinessException("El proveedor seleccionado no existe.");
        if (!Reglas.telefonoValido(dto.telefono())) throw new BusinessException(Reglas.MSG_TELEFONO);
        if (dto.telefono() != null && !dto.telefono().isBlank()
                && personalRepository.contarTelefono(Reglas.digitos(dto.telefono()), Reglas.idOMenosUno(id)) > 0) {
            throw new DuplicateResourceException("El teléfono " + dto.telefono().trim() + " ya está registrado para otra persona.");
        }
        boolean conduce = !"GUIA".equals(dto.rol());
        String licencia = dto.numLicencia() == null || dto.numLicencia().isBlank() ? null : dto.numLicencia().trim().toUpperCase();
        String categoria = dto.catLicencia() == null || dto.catLicencia().isBlank() ? null : dto.catLicencia();
        if (conduce && (licencia == null || categoria == null || dto.vencLicencia() == null)) {
            throw new BusinessException("Quien conduce necesita número, categoría y vencimiento de licencia.");
        }
        if (licencia != null) {
            Optional<Personal> mismo = personalRepository.findByNumLicenciaIgnoreCase(licencia);
            if (mismo.isPresent() && !mismo.get().getPersonalId().equals(id)) {
                throw new DuplicateResourceException("Ya existe una persona con la licencia " + licencia + ".");
            }
        }
        p.setProveedorId(dto.proveedorId());
        p.setNombreCompleto(dto.nombreCompleto().trim());
        p.setRol(dto.rol());
        p.setTelefono(dto.telefono() == null || dto.telefono().isBlank() ? null : dto.telefono().trim());
        p.setNumLicencia(licencia);
        p.setCatLicencia(licencia == null ? null : categoria);
        p.setVencLicencia(licencia == null ? null : dto.vencLicencia());
        p.setEstado(dto.estado());
    }
}
