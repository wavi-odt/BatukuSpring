package org.example.batuku.storage;

public enum FileCategory {
    AVATAR("avatars"),
    AUDIO("audio"),
    COVER("covers"),
    SELFIE("verification/selfies"),
    ID_DOCUMENT("verification/documents");

    private final String folder;

    FileCategory(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }
}
