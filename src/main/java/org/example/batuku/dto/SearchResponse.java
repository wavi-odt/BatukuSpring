package org.example.batuku.dto;

import java.util.List;

public record SearchResponse(
        List<ArtistResult> artists,
        List<TrackResult> tracks,
        List<PlaylistResult> playlists,
        List<UserResult> users
) {
    public record ArtistResult(Long id, String name, String imageUrl, String genre) {}
    public record TrackResult(Long id, String title, String artist, String imageUrl, String duration) {}
    public record PlaylistResult(Long id, String name, String imageUrl, Integer trackCount) {}
    public record UserResult(Long id, String name, String handle, String imageUrl) {}
}
