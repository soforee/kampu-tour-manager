package pe.edu.upeu.lp2_gp2.services;

import org.springframework.data.domain.PageRequest;
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

@Service
public class ClienteServiceImpl implements IClienteService {
    private static final int MAX_RESULTADOS = 8;

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
    public List<ClienteDTO> buscar(String texto) {
        String t = texto == null ? "" : texto.trim();
        if (t.length() < 2) return List.of();
        if (t.length() > 100) t = t.substring(0, 100);
        String patron = "%" + t.toUpperCase().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return clienteRepository.buscar(patron, PageRequest.of(0, MAX_RESULTADOS)).stream().map(ClienteDTO::desde).toList();
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
        String errorTelefono = Reglas.errorTelefonoPais(dto.telefono());
        if (errorTelefono != null) throw new BusinessException(errorTelefono);
        String documento = dto.numeroDocumento().trim();
        Optional<Cliente> existente = clienteRepository.findByNumeroDocumento(documento);
        if (existente.isPresent() && !existente.get().getClienteId().equals(id)) {
            throw new DuplicateResourceException("Ya existe un cliente con ese número de documento.");
        }
        c.setNombreCompleto(dto.nombreCompleto().trim());
        c.setTipoDocumento(dto.tipoDocumento());
        c.setNumeroDocumento(documento);
        c.setEmail(dto.email().trim().toLowerCase());
        c.setTelefono(dto.telefono().trim());
    }
}
