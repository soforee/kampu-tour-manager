package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.VehiculoDTO;
import pe.edu.upeu.lp2_gp2.dto.VehiculoRequestDTO;
import pe.edu.upeu.lp2_gp2.entity.Vehiculo;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.DuplicateResourceException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.OperacionTransporteRepository;
import pe.edu.upeu.lp2_gp2.repository.ProveedorRepository;
import pe.edu.upeu.lp2_gp2.repository.VehiculoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class VehiculoServiceImpl implements IVehiculoService {
    private final VehiculoRepository vehiculoRepository;
    private final ProveedorRepository proveedorRepository;
    private final OperacionTransporteRepository operacionRepository;

    public VehiculoServiceImpl(VehiculoRepository vehiculoRepository, ProveedorRepository proveedorRepository,
                               OperacionTransporteRepository operacionRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.proveedorRepository = proveedorRepository;
        this.operacionRepository = operacionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoDTO> listarTodo() {
        return vehiculoRepository.findAllByOrderByVehiculoIdAsc().stream().map(VehiculoDTO::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehiculoDTO buscarPorId(Long id) {
        return VehiculoDTO.desde(obtener(id));
    }

    @Override
    @Transactional
    public VehiculoDTO crear(VehiculoRequestDTO dto) {
        Vehiculo v = new Vehiculo();
        copiar(dto, v, null);
        return VehiculoDTO.desde(vehiculoRepository.saveAndFlush(v));
    }

    @Override
    @Transactional
    public VehiculoDTO actualizar(Long id, VehiculoRequestDTO dto) {
        Vehiculo v = obtener(id);
        if (!v.getProveedorId().equals(dto.proveedorId()) && operacionRepository.existsByVehiculoId(id)) {
            throw new BusinessException("El vehículo tiene operaciones registradas: no puede cambiar de proveedor.");
        }
        copiar(dto, v, id);
        return VehiculoDTO.desde(vehiculoRepository.saveAndFlush(v));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Vehiculo v = obtener(id);
        if (operacionRepository.existsByVehiculoId(id)) {
            throw new BusinessException("El vehículo tiene operaciones registradas. Puedes marcarlo como INACTIVO.");
        }
        vehiculoRepository.delete(v);
    }

    private Vehiculo obtener(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con Id: " + id));
    }

    private void copiar(VehiculoRequestDTO dto, Vehiculo v, Long id) {
        if (!proveedorRepository.existsById(dto.proveedorId())) throw new BusinessException("El proveedor seleccionado no existe.");
        String placa = dto.placa().trim().toUpperCase();
        Optional<Vehiculo> mismo = vehiculoRepository.findByPlacaIgnoreCase(placa);
        if (mismo.isPresent() && !mismo.get().getVehiculoId().equals(id)) {
            throw new DuplicateResourceException("Ya existe un vehículo con la placa " + placa + ".");
        }
        v.setProveedorId(dto.proveedorId());
        v.setPlaca(placa);
        v.setTipoVehiculo(dto.tipoVehiculo());
        v.setMarcaModelo(dto.marcaModelo() == null || dto.marcaModelo().isBlank() ? null : dto.marcaModelo().trim());
        v.setCapacidadPax(dto.capacidadPax());
        v.setProxMantenimiento(dto.proxMantenimiento());
        v.setEstado(dto.estado());
    }
}
