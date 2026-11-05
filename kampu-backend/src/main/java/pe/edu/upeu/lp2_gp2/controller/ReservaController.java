package pe.edu.upeu.lp2_gp2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.dto.AnulacionRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.BoletaEnvioDTO;
import pe.edu.upeu.lp2_gp2.dto.PasajeroReservaDTO;
import pe.edu.upeu.lp2_gp2.dto.ReservaDTO;
import pe.edu.upeu.lp2_gp2.dto.ReservaRequestDTO;
import pe.edu.upeu.lp2_gp2.services.IBoletaService;
import pe.edu.upeu.lp2_gp2.services.IReservaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {
    private final IReservaService reservaService;
    private final IBoletaService boletaService;

    public ReservaController(IReservaService reservaService, IBoletaService boletaService) {
        this.reservaService = reservaService;
        this.boletaService = boletaService;
    }

    @GetMapping
    public ResponseEntity<List<ReservaDTO>> listar() {
        return ResponseEntity.ok(reservaService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.buscarPorId(id));
    }

    @GetMapping("/{id}/pasajeros")
    public ResponseEntity<List<PasajeroReservaDTO>> pasajeros(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.listarPasajeros(id));
    }

    // Vista previa de la boleta (el mismo HTML que se envía por correo)
    @GetMapping("/{id}/boleta")
    public ResponseEntity<String> verBoleta(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(boletaService.generarHtml(id));
    }

    // Reenvío manual de la boleta al correo del cliente
    @PostMapping("/{id}/boleta")
    public ResponseEntity<BoletaEnvioDTO> enviarBoleta(@PathVariable Long id) {
        String destino = boletaService.enviar(id);
        return ResponseEntity.ok(new BoletaEnvioDTO(destino, java.time.LocalDateTime.now()));
    }

    @PostMapping
    public ResponseEntity<ReservaDTO> crear(@Valid @RequestBody ReservaRequestDTO dto) {
        return new ResponseEntity<>(reservaService.crear(dto), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<ReservaDTO> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.confirmar(id));
    }

    @PatchMapping("/{id}/completar")
    public ResponseEntity<ReservaDTO> completar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.completar(id));
    }

    @PatchMapping("/{id}/anular")
    public ResponseEntity<ReservaDTO> anular(@PathVariable Long id,
                                             @Valid @RequestBody(required = false) AnulacionRequestDTO dto) {
        return ResponseEntity.ok(reservaService.anular(id, dto == null ? null : dto.usuarioId(),
                dto == null ? null : dto.formaReembolso()));
    }
}
