package pe.edu.upeu.lp2_gp2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.dto.*;
import pe.edu.upeu.lp2_gp2.services.IOperacionTransporteService;

import java.util.List;

// Tráfico de transporte (OPERACIONES_TRANSPORTE). El usuario del historial sale de la sesión.
@RestController
@RequestMapping("/api/v1/operaciones")
public class OperacionTransporteController {
    private final IOperacionTransporteService operacionService;

    public OperacionTransporteController(IOperacionTransporteService operacionService) {
        this.operacionService = operacionService;
    }

    // Tablero; ?salidaId= filtra las operaciones de una salida
    @GetMapping
    public ResponseEntity<List<OperacionDTO>> listar(@RequestParam(required = false) Long salidaId) {
        return ResponseEntity.ok(operacionService.listar(salidaId));
    }

    // Estados y transiciones permitidas (EstadoOperacion)
    @GetMapping("/estados")
    public ResponseEntity<List<EstadoOperacionDTO>> estados() {
        return ResponseEntity.ok(operacionService.estados());
    }

    // Duración real del trayecto: OSRM (OpenStreetMap) o coordenadas de los lugares
    @GetMapping("/estimar-duracion")
    public ResponseEntity<EstimacionDuracionDTO> estimarDuracion(@RequestParam Long origenId, @RequestParam Long destinoId) {
        return ResponseEntity.ok(operacionService.estimarDuracion(origenId, destinoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperacionDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(operacionService.buscarPorId(id));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialOperacionDTO>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(operacionService.historial(id));
    }

    @GetMapping("/{id}/candidatos")
    public ResponseEntity<List<RecursoCandidatoDTO>> candidatos(@PathVariable Long id) {
        return ResponseEntity.ok(operacionService.candidatos(id));
    }

    @PostMapping
    public ResponseEntity<OperacionDTO> crear(@Valid @RequestBody OperacionRequestDTO dto) {
        return new ResponseEntity<>(operacionService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OperacionDTO> actualizar(@PathVariable Long id, @Valid @RequestBody OperacionRequestDTO dto) {
        return ResponseEntity.ok(operacionService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<OperacionDTO> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequestDTO dto) {
        return ResponseEntity.ok(operacionService.cambiarEstado(id, dto));
    }

    @PostMapping("/{id}/asignar-automatico")
    public ResponseEntity<OperacionDTO> asignarAutomatico(@PathVariable Long id) {
        return ResponseEntity.ok(operacionService.asignarAutomatico(id));
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<OperacionDTO> asignar(@PathVariable Long id, @Valid @RequestBody AsignacionRequestDTO dto) {
        return ResponseEntity.ok(operacionService.asignar(id, dto));
    }
}
