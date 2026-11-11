package pe.edu.upeu.lp2_gp2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.lp2_gp2.dto.ResumenPaqueteDTO;
import pe.edu.upeu.lp2_gp2.repository.VistaResumenPaqueteRepository;

import java.util.List;

// Vista interna por rol: VW_RESUMEN_PAQUETES_POR_ROL (la auditoría está en AuditoriaController)
@RestController
public class ResumenPaquetesController {
    private final VistaResumenPaqueteRepository resumenRepository;

    public ResumenPaquetesController(VistaResumenPaqueteRepository resumenRepository) {
        this.resumenRepository = resumenRepository;
    }

    // ADMIN/VENDEDOR: todas las salidas con montos; OPERADOR: su cola (por empezar / en curso) sin montos
    @GetMapping("/api/v1/resumen-paquetes")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ResumenPaqueteDTO>> resumen() {
        return ResponseEntity.ok(resumenRepository.findAllByOrderByFechaInicioAsc().stream().map(ResumenPaqueteDTO::desde).toList());
    }

}
