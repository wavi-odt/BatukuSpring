package org.example.batuku.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file, FileCategory category);

    String resolveUrl(String key, FileCategory category);

    void delete(String key, FileCategory category);
}
