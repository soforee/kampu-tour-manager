package pe.edu.upeu.lp2_gp2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.dto.PersonalDTO;
import pe.edu.upeu.lp2_gp2.dto.PersonalRequestDTO;
import pe.edu.upeu.lp2_gp2.services.IPersonalService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/personal")
public class PersonalController {
    private final IPersonalService personalService;

    public PersonalController(IPersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping
    public ResponseEntity<List<PersonalDTO>> listar() {
        return ResponseEntity.ok(personalService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonalDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(personalService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<PersonalDTO> crear(@Valid @RequestBody PersonalRequestDTO dto) {
        return new ResponseEntity<>(personalService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonalDTO> actualizar(@PathVariable Long id, @Valid @RequestBody PersonalRequestDTO dto) {
        return ResponseEntity.ok(personalService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        personalService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
