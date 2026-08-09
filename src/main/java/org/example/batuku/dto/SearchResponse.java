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

    /**
     * artistProfileId: preenchido quando o utilizador tem um perfil de artista claimed.
     * Nesse caso o frontend deve navegar para /artists/{artistProfileId} e mostrar
     * o label "Artista" em vez do handle.
     */
    public record UserResult(Long id, String name, String handle, String imageUrl, Long artistProfileId) {}
}
