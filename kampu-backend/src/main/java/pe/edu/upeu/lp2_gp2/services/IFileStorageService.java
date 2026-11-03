package pe.edu.upeu.lp2_gp2.services;

import org.springframework.web.multipart.MultipartFile;

public interface IFileStorageService {
    void init();
    String store(MultipartFile file, String carpeta);
    void delete(String ruta);
}
