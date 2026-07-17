package org.example.batuku.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Component
public class SpotifyClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public SpotifyClient(RestClient restClient,
                         @Value("${batuku.spotify.client-id}") String clientId,
                         @Value("${batuku.spotify.client-secret}") String clientSecret) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private synchronized String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedToken;
        }

        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        SpotifyTokenResponse response = restClient.post()
                .uri("https://accounts.spotify.com/api/token")
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(SpotifyTokenResponse.class);

        cachedToken = response.accessToken();
        tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn());
        return cachedToken;
    }

    public List<SpotifyArtist> searchArtists(String query, int limit) {
        SpotifySearchResponse response = restClient.get()
                .uri("https://api.spotify.com/v1/search?q={q}&type=artist&limit={limit}", query, limit)
                .header("Authorization", "Bearer " + accessToken())
                .retrieve()
                .body(SpotifySearchResponse.class);
        return response.artists().items();
    }

    public SpotifyArtist getArtist(String spotifyArtistId) {
        return restClient.get()
                .uri("https://api.spotify.com/v1/artists/{id}", spotifyArtistId)
                .header("Authorization", "Bearer " + accessToken())
                .retrieve()
                .body(SpotifyArtist.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyArtist(
            String id,
            String name,
            List<String> genres,
            SpotifyImage[] images,
            @JsonProperty("external_urls") SpotifyExternalUrls externalUrls,
            SpotifyFollowers followers,
            Integer popularity
    ) {
        public String imageUrl() {
            return (images != null && images.length > 0) ? images[0].url() : null;
        }

        public String spotifyUrl() {
            return externalUrls != null ? externalUrls.spotify() : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyImage(String url) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyExternalUrls(String spotify) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyFollowers(Integer total) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyArtistPage(List<SpotifyArtist> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifySearchResponse(SpotifyArtistPage artists) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyTrack(
            String id,
            String name,
            @JsonProperty("preview_url") String previewUrl,
            @JsonProperty("duration_ms") Integer durationMs,
            @JsonProperty("external_urls") SpotifyExternalUrls externalUrls,
            SpotifyAlbum album
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SpotifyAlbum(SpotifyImage[] images) {
        public String coverUrl() {
            return (images != null && images.length > 0) ? images[0].url() : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyTopTracksResponse(List<SpotifyTrack> tracks) {}

    public List<SpotifyTrack> getTopTracks(String artistId, String market) {
        SpotifyTopTracksResponse response = restClient.get()
                .uri("https://api.spotify.com/v1/artists/{id}/top-tracks?market={market}", artistId, market)
                .header("Authorization", "Bearer " + accessToken())
                .retrieve()
                .body(SpotifyTopTracksResponse.class);
        return response.tracks();
    }
}
