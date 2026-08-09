package org.example.batuku.dto;

import org.example.batuku.domain.ArtistProfile;
import java.util.List;

public record ArtistMeResponse(
        Long id,
        String bio,
        String location,
        List<String> genres,
        List<String> languages,
        List<SocialLink> links
) {
    public record SocialLink(String kind, String handle) {}

    public static ArtistMeResponse from(ArtistProfile p) {
        List<SocialLink> links = p.getLinks() != null
                ? p.getLinks().stream()
                        .filter(l -> l.getKind() != null && !l.getKind().isBlank())
                        .map(l -> new SocialLink(l.getKind(), l.getHandle()))
                        .toList()
                : List.of();
        return new ArtistMeResponse(
                p.getId(),
                p.getBio(),
                p.getLocation(),
                p.getGenres() != null ? p.getGenres() : List.of(),
                p.getLanguages() != null ? p.getLanguages() : List.of(),
                links
        );
    }
}
