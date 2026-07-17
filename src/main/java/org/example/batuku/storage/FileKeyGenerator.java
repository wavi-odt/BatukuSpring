package org.example.batuku.storage;

import java.util.UUID;

final class FileKeyGenerator {

    private FileKeyGenerator() {}

    static String generate(FileCategory category, String originalFilename) {
        String ext = "";
        if (originalFilename != null) {
            int dotIdx = originalFilename.lastIndexOf('.');
            if (dotIdx >= 0) {
                String candidate = originalFilename.substring(dotIdx);
                if (candidate.matches("\\.[a-zA-Z0-9]{1,10}")) {
                    ext = candidate;
                }
            }
        }
        return category.getFolder() + "/" + UUID.randomUUID() + ext;
    }
}
