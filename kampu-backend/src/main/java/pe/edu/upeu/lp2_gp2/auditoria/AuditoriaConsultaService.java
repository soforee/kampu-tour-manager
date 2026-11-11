package pe.edu.upeu.lp2_gp2.auditoria;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lp2_gp2.entity.Auditoria;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;
import pe.edu.upeu.lp2_gp2.repository.AuditoriaRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta de la bitácora con filtros combinables. Filtrar por rol aprovecha la
 * partición de AUDITORIA por rol_usuario (Oracle solo lee esa partición).
 */
@Service
public class AuditoriaConsultaService {
    public static final int MAX_TAMANIO = 200;
    public static final int MAX_EXPORTAR = 5000;

    public record Filtro(LocalDate desde, LocalDate hasta, String rol, String accion, String entidad,
                         Long entidadId, String usuario, String texto) {
    }

    private final AuditoriaRepository repository;

    public AuditoriaConsultaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Auditoria> buscar(Filtro f, int pagina, int tamanio) {
        int t = Math.max(1, Math.min(tamanio, MAX_TAMANIO));
        return repository.findAll(especificacion(f), PageRequest.of(Math.max(0, pagina), t, orden()));
    }

    @Transactional(readOnly = true)
    public List<Auditoria> exportar(Filtro f) {
        return repository.findAll(especificacion(f), PageRequest.of(0, MAX_EXPORTAR, orden())).getContent();
    }

    private static Sort orden() {
        return Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("auditoriaId"));
    }

    private static Specification<Auditoria> especificacion(Filtro f) {
        if (f.desde() != null && f.hasta() != null && f.hasta().isBefore(f.desde())) {
            throw new BusinessException("La fecha 'hasta' no puede ser anterior a 'desde'.");
        }
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.desde() != null) p.add(cb.greaterThanOrEqualTo(root.get("fecha"), f.desde().atStartOfDay()));
            if (f.hasta() != null) p.add(cb.lessThan(root.get("fecha"), f.hasta().plusDays(1).atStartOfDay()));
            if (lleno(f.rol())) p.add(cb.equal(root.get("rolUsuario"), f.rol().trim().toUpperCase()));
            if (lleno(f.accion())) p.add(cb.equal(root.get("accion"), f.accion().trim().toUpperCase()));
            if (lleno(f.entidad())) p.add(cb.equal(root.get("entidad"), f.entidad().trim().toUpperCase()));
            if (f.entidadId() != null) p.add(cb.equal(root.get("entidadId"), f.entidadId()));
            if (lleno(f.usuario())) p.add(cb.equal(cb.lower(root.get("usuario")), f.usuario().trim().toLowerCase()));
            if (lleno(f.texto())) {
                String like = "%" + f.texto().trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("detalle")), like),
                        cb.like(cb.lower(root.get("campo")), like),
                        cb.like(cb.lower(root.get("valorAnterior")), like),
                        cb.like(cb.lower(root.get("valorNuevo")), like),
                        cb.like(cb.lower(root.get("usuario")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static boolean lleno(String s) {
        return s != null && !s.isBlank();
    }
}
