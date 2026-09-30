package org.example.batuku.dto;

public record SimilarArtistResponse(
        Long id,
        String name,
        String imageUrl,
        String genre,
        long totalPlays
) {}
