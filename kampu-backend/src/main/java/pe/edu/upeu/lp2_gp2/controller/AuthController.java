package pe.edu.upeu.lp2_gp2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.dto.LoginRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.RegistroRequestDTO;
import pe.edu.upeu.lp2_gp2.dto.SesionDTO;
import pe.edu.upeu.lp2_gp2.services.IAuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    // Personal interno (usuario) o cliente del portal (correo o documento)
    @PostMapping("/login")
    public ResponseEntity<SesionDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    // "Registrarse": autoregistro de clientes; devuelve la sesión iniciada
    @PostMapping("/registro")
    public ResponseEntity<SesionDTO> registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        return new ResponseEntity<>(authService.registrar(dto), HttpStatus.CREATED);
    }

    // Sesión vigente y permisos actualizados (el frontend la pide al recargar)
    @GetMapping("/sesion")
    public ResponseEntity<SesionDTO> sesion(@RequestHeader(value = "Authorization", required = false) String cabecera) {
        return ResponseEntity.ok(authService.sesion(cabecera != null && cabecera.startsWith("Bearer ") ? cabecera.substring(7).trim() : null));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String cabecera) {
        authService.logout(cabecera != null && cabecera.startsWith("Bearer ") ? cabecera.substring(7).trim() : null);
        return ResponseEntity.noContent().build();
    }
}
