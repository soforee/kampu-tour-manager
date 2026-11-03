package pe.edu.upeu.lp2_gp2.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements IFileStorageService {
    @Value("${storage.location}")
    private String storageLocation;

    private Path raiz() {
        return Paths.get(storageLocation).toAbsolutePath().normalize();
    }

    @Override
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(raiz());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar la carpeta de imágenes");
        }
    }

    @Override
    public String store(MultipartFile file, String carpeta) {
        if (file == null || file.isEmpty()) throw new BusinessException("Selecciona una imagen.");
        String extension = extension(file.getContentType());
        if (extension == null) throw new BusinessException("El archivo debe ser una imagen (JPG, PNG o WebP).");
        try {
            String ruta = carpeta + "/" + UUID.randomUUID() + extension;
            Path destino = raiz().resolve(ruta).normalize();
            Files.createDirectories(destino.getParent());
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return ruta;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo", e);
        }
    }

    @Override
    public void delete(String ruta) {
        if (ruta == null || ruta.isBlank()) return;
        Path archivo = raiz().resolve(ruta).normalize();
        if (!archivo.startsWith(raiz())) return;
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            System.err.println("No se pudo borrar el archivo: " + ruta);
        }
    }

    private String extension(String contentType) {
        if ("image/jpeg".equals(contentType)) return ".jpg";
        if ("image/png".equals(contentType)) return ".png";
        if ("image/webp".equals(contentType)) return ".webp";
        return null;
    }
}
