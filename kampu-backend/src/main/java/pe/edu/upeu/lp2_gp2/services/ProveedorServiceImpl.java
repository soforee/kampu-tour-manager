package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.ProveedorDTO;
import pe.edu.upeu.lp2_gp2.dto.ProveedorRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.Reglas;
import pe.edu.upeu.lp2_gp2.entity.Personal;
import pe.edu.upeu.lp2_gp2.entity.Proveedor;
import pe.edu.upeu.lp2_gp2.entity.Vehiculo;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.DuplicateResourceException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.PersonalRepository;
import pe.edu.upeu.lp2_gp2.repository.ProveedorRepository;
import pe.edu.upeu.lp2_gp2.repository.VehiculoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Proveedores: Kampu Tours (es_propio = 1, único) y terceros. Tanto la flota propia
 * como la de terceros cuelgan de un proveedor, así no hace falta inventar una
 * "empresa de transporte" para registrar los vehículos de Kampu.
 */
@Service
public class ProveedorServiceImpl implements IProveedorService {
    private final ProveedorRepository proveedorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PersonalRepository personalRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository, VehiculoRepository vehiculoRepository,
                                PersonalRepository personalRepository) {
        this.proveedorRepository = proveedorRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.personalRepository = personalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDTO> listarTodo() {
        List<Vehiculo> vehiculos = vehiculoRepository.findAll();
        List<Personal> personal = personalRepository.findAll();
        return proveedorRepository.findAllByOrderByEsPropioDescProveedorIdAsc().stream()
                .map(p -> dto(p, vehiculos, personal)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorDTO buscarPorId(Long id) {
        return dto(obtener(id), vehiculoRepository.findAll(), personalRepository.findAll());
    }

    @Override
    @Transactional
    public ProveedorDTO crear(ProveedorRequestDTO dto) {
        Proveedor p = new Proveedor();
        copiar(dto, p, null);
        return buscarPorIdInterno(proveedorRepository.saveAndFlush(p));
    }

    @Override
    @Transactional
    public ProveedorDTO actualizar(Long id, ProveedorRequestDTO dto) {
        Proveedor p = obtener(id);
        copiar(dto, p, id);
        return buscarPorIdInterno(proveedorRepository.saveAndFlush(p));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Proveedor p = obtener(id);
        if (vehiculoRepository.existsByProveedorId(id) || personalRepository.existsByProveedorId(id)) {
            throw new BusinessException("El proveedor tiene vehículos o personal registrados. Puedes marcarlo como INACTIVO.");
        }
        proveedorRepository.delete(p);
    }

    private ProveedorDTO buscarPorIdInterno(Proveedor p) {
        return dto(p, vehiculoRepository.findAll(), personalRepository.findAll());
    }

    private ProveedorDTO dto(Proveedor p, List<Vehiculo> vehiculos, List<Personal> personal) {
        int flota = 0, capacidad = 0, conductores = 0, guias = 0;
        for (Vehiculo v : vehiculos) {
            if (!p.getProveedorId().equals(v.getProveedorId())) continue;
            flota++;
            if ("OPERATIVO".equals(v.getEstado())) capacidad += v.getCapacidadPax();
        }
        for (Personal x : personal) {
            if (!p.getProveedorId().equals(x.getProveedorId()) || "INACTIVO".equals(x.getEstado())) continue;
            if (!"GUIA".equals(x.getRol())) conductores++;
            if (!"CONDUCTOR".equals(x.getRol())) guias++;
        }
        return ProveedorDTO.desde(p, flota, capacidad, conductores, guias);
    }

    private Proveedor obtener(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con Id: " + id));
    }

    private void copiar(ProveedorRequestDTO dto, Proveedor p, Long id) {
        if (!Reglas.telefonoValido(dto.telefono())) throw new BusinessException(Reglas.MSG_TELEFONO);
        String ruc = dto.ruc().trim();
        Optional<Proveedor> mismo = proveedorRepository.findByRuc(ruc);
        if (mismo.isPresent() && !mismo.get().getProveedorId().equals(id)) {
            throw new DuplicateResourceException("Ya existe un proveedor con el RUC " + ruc + ".");
        }
        if (dto.telefono() != null && !dto.telefono().isBlank()
                && proveedorRepository.contarTelefono(Reglas.digitos(dto.telefono()), Reglas.idOMenosUno(id)) > 0) {
            throw new DuplicateResourceException("El teléfono " + dto.telefono().trim() + " ya está registrado para otro proveedor.");
        }
        if (dto.esPropio()) {
            Optional<Proveedor> propio = proveedorRepository.findFirstByEsPropio(1);
            if (propio.isPresent() && !propio.get().getProveedorId().equals(id)) {
                throw new BusinessException("Ya existe el proveedor propio (" + propio.get().getNombreComercial() + ").");
            }
        } else if (id != null && Integer.valueOf(1).equals(p.getEsPropio())) {
            throw new BusinessException("Kampu Tours debe seguir siendo el proveedor propio.");
        }
        p.setNombreComercial(dto.nombreComercial().trim());
        p.setRuc(ruc);
        p.setTelefono(dto.telefono() == null || dto.telefono().isBlank() ? null : dto.telefono().trim());
        p.setEsPropio(dto.esPropio() ? 1 : 0);
        p.setEstado(dto.estado());
    }
}
