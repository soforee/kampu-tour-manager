package pe.edu.upeu.lp2_gp2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.dto.EjecucionRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.ManifiestoDTO;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaDTO;
import pe.edu.upeu.lp2_gp2.repository.PkgSeguridadRepository;
import pe.edu.upeu.lp2_gp2.seguridad.SesionActual;
import pe.edu.upeu.lp2_gp2.dto.SalidaProgramadaRequestDTO;
import pe.edu.upeu.lp2_gp2.services.ISalidaProgramadaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salidas")
public class SalidaProgramadaController {
    private final ISalidaProgramadaService salidaService;
    private final PkgSeguridadRepository seguridad;

    public SalidaProgramadaController(ISalidaProgramadaService salidaService, PkgSeguridadRepository seguridad) {
        this.salidaService = salidaService;
        this.seguridad = seguridad;
    }

    @GetMapping
    public ResponseEntity<List<SalidaProgramadaDTO>> listar() {
        return ResponseEntity.ok(salidaService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalidaProgramadaDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(salidaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<SalidaProgramadaDTO> crear(@Valid @RequestBody SalidaProgramadaRequestDTO dto) {
        return new ResponseEntity<>(salidaService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalidaProgramadaDTO> actualizar(@PathVariable Long id, @Valid @RequestBody SalidaProgramadaRequestDTO dto) {
        return ResponseEntity.ok(salidaService.actualizar(id, dto));
    }

    // Operador / Ventas: en ejecución (CONFIRMADA) o terminada (COMPLETADA)
    @PatchMapping("/{id}/ejecucion")
    public ResponseEntity<SalidaProgramadaDTO> ejecucion(@PathVariable Long id, @Valid @RequestBody EjecucionRequestDTO dto) {
        return ResponseEntity.ok(salidaService.actualizarEjecucion(id, dto.estado()));
    }

    // Manifiesto: pasajeros de la salida. Los datos de pago solo van a quien puede ver montos.
    @GetMapping("/{id}/manifiesto")
    public ResponseEntity<ManifiestoDTO> manifiesto(@PathVariable Long id) {
        boolean conMontos = seguridad.puede(SesionActual.rol(), "VER_MONTOS");
        return ResponseEntity.ok(salidaService.manifiesto(id, conMontos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        salidaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
