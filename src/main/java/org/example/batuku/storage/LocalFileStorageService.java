package org.example.batuku.storage;

import org.example.batuku.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(name = "batuku.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path baseDir;
    private final String publicBaseUrl;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.baseDir = Path.of(storageProperties.getLocal().getBaseDir()).toAbsolutePath();
        this.publicBaseUrl = storageProperties.getLocal().getPublicBaseUrl();
    }

    @Override
    public String store(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O ficheiro não pode estar vazio.");
        }
        String key = FileKeyGenerator.generate(category, file.getOriginalFilename());
        Path target = baseDir.resolve(key);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao guardar o ficheiro.", e);
        }
        return key;
    }

    @Override
    public String resolveUrl(String key, FileCategory category) {
        return publicBaseUrl + "/" + key;
    }

    @Override
    public void delete(String key, FileCategory category) {
        try {
            Files.deleteIfExists(baseDir.resolve(key));
        } catch (IOException e) {
            // ignorar — ficheiro pode já não existir
        }
    }
}
