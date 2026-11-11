package pe.edu.upeu.lp2_gp2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.dto.InformarPagoRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.PagoDTO;
import pe.edu.upeu.lp2_gp2.dto.PortalDetalleDTO;
import pe.edu.upeu.lp2_gp2.dto.PortalPaqueteDTO;
import pe.edu.upeu.lp2_gp2.services.PortalClienteService;

import java.util.List;

// Portal del cliente: sus paquetes con estado de ejecución y estado de pago
@RestController
@RequestMapping("/api/v1/portal")
public class PortalClienteController {
    private final PortalClienteService portalService;

    public PortalClienteController(PortalClienteService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/paquetes")
    public ResponseEntity<List<PortalPaqueteDTO>> misPaquetes() {
        return ResponseEntity.ok(portalService.misPaquetes());
    }

    @GetMapping("/paquetes/{reservaId}")
    public ResponseEntity<PortalDetalleDTO> detalle(@PathVariable Long reservaId) {
        return ResponseEntity.ok(portalService.detalle(reservaId));
    }

    @PostMapping("/paquetes/{reservaId}/pagos")
    public ResponseEntity<PagoDTO> informarPago(@PathVariable Long reservaId, @Valid @RequestBody InformarPagoRequestDTO dto) {
        return new ResponseEntity<>(portalService.informarPago(reservaId, dto), HttpStatus.CREATED);
    }
}
