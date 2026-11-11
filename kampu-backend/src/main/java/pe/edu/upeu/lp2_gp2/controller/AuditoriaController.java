package pe.edu.upeu.lp2_gp2.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.lp2_gp2.auditoria.AuditoriaConsultaService;
import pe.edu.upeu.lp2_gp2.auditoria.AuditoriaConsultaService.Filtro;
import pe.edu.upeu.lp2_gp2.dto.AuditoriaDTO;
import pe.edu.upeu.lp2_gp2.dto.PaginaDTO;
import pe.edu.upeu.lp2_gp2.entity.Auditoria;
import pe.edu.upeu.lp2_gp2.repository.AuditoriaRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Sección "Auditoría" (solo ADMIN: SeguridadInterceptor exige VER_AUDITORIA en /auditoria/**).
 *   GET /auditoria?desde=&hasta=&rol=&accion=&entidad=&entidadId=&usuario=&q=&pagina=0&tamanio=50
 *   GET /auditoria/filtros   -> valores para los combos (entidades y usuarios presentes en la bitácora)
 *   GET /auditoria/exportar  -> CSV con los mismos filtros (máx. 5000 filas)
 */
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {
    private final AuditoriaConsultaService consulta;
    private final AuditoriaRepository repository;

    public AuditoriaController(AuditoriaConsultaService consulta, AuditoriaRepository repository) {
        this.consulta = consulta;
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<PaginaDTO<AuditoriaDTO>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio) {
        Filtro f = new Filtro(desde, hasta, rol, accion, entidad, entidadId, usuario, q);
        return ResponseEntity.ok(PaginaDTO.de(consulta.buscar(f, pagina, tamanio), AuditoriaDTO::desde));
    }

    @GetMapping("/filtros")
    public ResponseEntity<Map<String, List<String>>> filtros() {
        return ResponseEntity.ok(Map.of("entidades", repository.entidades(), "usuarios", repository.usuarios()));
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String q) {
        List<Auditoria> filas = consulta.exportar(new Filtro(desde, hasta, rol, accion, entidad, entidadId, usuario, q));
        StringBuilder csv = new StringBuilder("﻿id;fecha;usuario;rol;accion;entidad;entidad_id;campo;valor_anterior;valor_nuevo;detalle;origen\n");
        for (Auditoria a : filas) {
            csv.append(String.join(";", s(a.getAuditoriaId()), s(a.getFecha()), c(a.getUsuario()), c(a.getRolUsuario()),
                    c(a.getAccion()), c(a.getEntidad()), s(a.getEntidadId()), c(a.getCampo()), c(a.getValorAnterior()),
                    c(a.getValorNuevo()), c(a.getDetalle()), c(a.getOrigen()))).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String s(Object o) {
        return o == null ? "" : o.toString();
    }

    // Celda CSV: comillas si hace falta
    private static String c(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"").replace('\n', ' ') + "\"";
    }
}
