package org.example.batuku.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ArtistGenresRequest {

    @NotNull
    @Size(max = 3, message = "Máximo 3 géneros.")
    private List<String> genres;

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }
}
