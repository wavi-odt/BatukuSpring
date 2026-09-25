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
        long monthlyListeners,
        int tracksCount,
        List<TrackItem> tracks,
        List<String> genres,
        List<String> languages,
        List<LinkItem> links,
        Long userId
) {
    public record TrackItem(Long id, String title, String imageUrl, long plays, String duration) {}
    public record LinkItem(String kind, String handle) {}
}
