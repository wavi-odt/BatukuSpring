package org.example.batuku.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ArtistLanguagesRequest {

    @NotNull
    private List<String> languages;

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }
}
