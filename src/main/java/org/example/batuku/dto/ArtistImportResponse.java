package org.example.batuku.dto;

import org.example.batuku.domain.ArtistProfile;

import java.util.List;

public record ArtistImportResponse(
        Long id,
        String spotifyArtistId,
        String name,
        String imageUrl,
        String thumbnailUrl,
        String spotifyUrl,
        List<String> genres,
        List<String> unmappedGenres,
        Integer popularity,
        Integer followerCount,
        boolean claimed,
        int tracksImported,
        int tracksUpdated,
        int tracksSkipped
) {
    public static ArtistImportResponse from(ArtistProfile p, int tracksImported, int tracksUpdated, int tracksSkipped, List<String> unmappedGenres) {
        return new ArtistImportResponse(
                p.getId(),
                p.getSpotifyArtistId(),
                p.getName(),
                p.getImageUrl(),
                p.getThumbnailUrl(),
                p.getSpotifyUrl(),
                p.getGenres(),
                unmappedGenres != null ? unmappedGenres : List.of(),
                p.getPopularity(),
                p.getFollowerCount(),
                p.isClaimed(),
                tracksImported,
                tracksUpdated,
                tracksSkipped
        );
    }
}
