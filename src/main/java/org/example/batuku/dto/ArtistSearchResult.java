package org.example.batuku.dto;

import org.example.batuku.services.SpotifyClient;

import java.util.List;

public record ArtistSearchResult(
        String id,
        String name,
        String imageUrl,
        List<String> genres,
        Integer followers,
        Integer popularity,
        String spotifyUrl,
        boolean imported
) {
    public static ArtistSearchResult from(SpotifyClient.SpotifyArtist artist, boolean imported) {
        return new ArtistSearchResult(
                artist.id(),
                artist.name(),
                artist.imageUrl(),
                artist.genres(),
                artist.followers() != null ? artist.followers().total() : null,
                artist.popularity(),
                artist.spotifyUrl(),
                imported
        );
    }
}
