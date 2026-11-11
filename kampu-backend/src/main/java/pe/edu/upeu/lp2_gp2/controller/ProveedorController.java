package pe.edu.upeu.lp2_gp2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.dto.ProveedorDTO;
import pe.edu.upeu.lp2_gp2.dto.ProveedorRequestDTO;
import pe.edu.upeu.lp2_gp2.services.IProveedorService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proveedores")
public class ProveedorController {
    private final IProveedorService proveedorService;

    public ProveedorController(IProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public ResponseEntity<List<ProveedorDTO>> listar() {
        return ResponseEntity.ok(proveedorService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProveedorDTO> crear(@Valid @RequestBody ProveedorRequestDTO dto) {
        return new ResponseEntity<>(proveedorService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequestDTO dto) {
        return ResponseEntity.ok(proveedorService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        proveedorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
