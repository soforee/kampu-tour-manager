package pe.edu.upeu.lp2_gp2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.dto.ChoferDTO;
import pe.edu.upeu.lp2_gp2.dto.ChoferRequestDTO;
import pe.edu.upeu.lp2_gp2.services.IChoferService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/choferes")
public class ChoferController {
    private final IChoferService choferService;

    public ChoferController(IChoferService choferService) {
        this.choferService = choferService;
    }

    @GetMapping
    public ResponseEntity<List<ChoferDTO>> listar() {
        return ResponseEntity.ok(choferService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChoferDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(choferService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ChoferDTO> crear(@Valid @RequestBody ChoferRequestDTO dto) {
        return new ResponseEntity<>(choferService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChoferDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ChoferRequestDTO dto) {
        return ResponseEntity.ok(choferService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        choferService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
