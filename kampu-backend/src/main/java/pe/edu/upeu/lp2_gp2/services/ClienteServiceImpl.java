package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.dto.ClienteDTO;
import pe.edu.upeu.lp2_gp2.dto.ClienteRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.Reglas;
import pe.edu.upeu.lp2_gp2.entity.Cliente;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.exception.DuplicateResourceException;
import pe.edu.upeu.lp2_gp2.exception.ResourceNotFoundException;
import pe.edu.upeu.lp2_gp2.repository.ClienteRepository;
import pe.edu.upeu.lp2_gp2.repository.ReservaRepository;

import java.util.List;
import java.util.Optional;

import static pe.edu.upeu.lp2_gp2.util.Texto.limpio;

@Service
public class ClienteServiceImpl implements IClienteService {
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository, ReservaRepository reservaRepository) {
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDTO> listarTodo() {
        return clienteRepository.findAllByOrderByClienteIdAsc().stream().map(ClienteDTO::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteDTO buscarPorId(Long id) {
        return ClienteDTO.desde(obtener(id));
    }

    @Override
    @Transactional
    public ClienteDTO crear(ClienteRequestDTO dto) {
        Cliente c = new Cliente();
        copiar(dto, c, null);
        return ClienteDTO.desde(clienteRepository.save(c));
    }

    @Override
    @Transactional
    public ClienteDTO actualizar(Long id, ClienteRequestDTO dto) {
        Cliente c = obtener(id);
        copiar(dto, c, id);
        return ClienteDTO.desde(clienteRepository.save(c));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente c = obtener(id);
        if (reservaRepository.existsByClienteId(id)) throw new BusinessException("El cliente tiene reservas registradas.");
        clienteRepository.delete(c);
    }

    private Cliente obtener(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con Id: " + id));
    }

    private void copiar(ClienteRequestDTO dto, Cliente c, Long id) {
        if (!Reglas.documentoValido(dto.tipoDocumento(), dto.numeroDocumento())) {
            throw new BusinessException(Reglas.MSG_DOCUMENTO);
        }
        if (!Reglas.telefonoValido(dto.telefono())) throw new BusinessException(Reglas.MSG_TELEFONO);
        String documento = dto.numeroDocumento().trim();
        Optional<Cliente> existente = clienteRepository.findByNumeroDocumento(documento);
        if (existente.isPresent() && !existente.get().getClienteId().equals(id)) {
            throw new DuplicateResourceException("Ya existe un cliente con ese número de documento.");
        }
        c.setNombreCompleto(dto.nombreCompleto().trim());
        c.setTipoDocumento(dto.tipoDocumento());
        c.setNumeroDocumento(documento);
        c.setEmail(limpio(dto.email()));
        c.setTelefono(limpio(dto.telefono()));
    }
}
