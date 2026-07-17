package org.example.batuku.dto;

import java.util.List;

public record ArtistDetailResponse(
        Long id,
        String name,
        String imageUrl,
        String genre,
        String city,
        String bio,
        long followers,
        int tracksCount,
        List<TrackItem> tracks
) {
    public record TrackItem(Long id, String title, String imageUrl, long plays, String duration) {}
}
