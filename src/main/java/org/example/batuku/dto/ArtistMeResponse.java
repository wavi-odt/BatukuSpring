package org.example.batuku.dto;

import org.example.batuku.domain.ArtistProfile;
import java.util.List;

public record ArtistMeResponse(
        Long id,
        String bio,
        String location,
        List<String> genres,
        List<String> languages
) {
    public static ArtistMeResponse from(ArtistProfile p) {
        return new ArtistMeResponse(
                p.getId(),
                p.getBio(),
                p.getLocation(),
                p.getGenres() != null ? p.getGenres() : List.of(),
                p.getLanguages() != null ? p.getLanguages() : List.of()
        );
    }
}
