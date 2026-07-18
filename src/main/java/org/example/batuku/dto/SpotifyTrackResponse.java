package org.example.batuku.dto;

public record SpotifyTrackResponse(
        String spotifyId,
        String name,
        String previewUrl,
        Integer durationMs,
        String spotifyUrl,
        String coverUrl
) {}
