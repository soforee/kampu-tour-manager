package pe.edu.upeu.lp2_gp2.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// Página de resultados: contenido + total para la paginación del frontend
public record PaginaDTO<T>(List<T> contenido, long total, int pagina, int tamanio) {
    public static <E, T> PaginaDTO<T> de(Page<E> p, Function<E, T> mapa) {
        return new PaginaDTO<>(p.getContent().stream().map(mapa).toList(), p.getTotalElements(), p.getNumber(), p.getSize());
    }
}
