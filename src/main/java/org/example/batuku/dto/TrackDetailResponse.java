package org.example.batuku.dto;

public record TrackDetailResponse(
        Long id,
        String title,
        String artist,
        Long artistId,
        String imageUrl,
        String genre,
        String duration,
        long plays
) {}
