package pe.edu.upeu.lp2_gp2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.dto.VehiculoDTO;
import pe.edu.upeu.lp2_gp2.dto.VehiculoRequestDTO;
import pe.edu.upeu.lp2_gp2.services.IVehiculoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {
    private final IVehiculoService vehiculoService;

    public VehiculoController(IVehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @GetMapping
    public ResponseEntity<List<VehiculoDTO>> listar() {
        return ResponseEntity.ok(vehiculoService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculoDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(vehiculoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<VehiculoDTO> crear(@Valid @RequestBody VehiculoRequestDTO dto) {
        return new ResponseEntity<>(vehiculoService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehiculoDTO> actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequestDTO dto) {
        return ResponseEntity.ok(vehiculoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vehiculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
