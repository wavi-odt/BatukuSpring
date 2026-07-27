package org.example.batuku.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import org.example.batuku.exception.SpotifyApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

@Component
public class SpotifyClient {

    private static final Logger log = LoggerFactory.getLogger(SpotifyClient.class);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String market;

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public SpotifyClient(RestClient restClient,
                         @Value("${batuku.spotify.client-id}") String clientId,
                         @Value("${batuku.spotify.client-secret}") String clientSecret,
                         @Value("${batuku.spotify.market:PT}") String market) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.market = market;
    }

    @PostConstruct
    void validateCredentials() {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            log.warn("Spotify credentials not configured (SPOTIFY_CLIENT_ID / SPOTIFY_CLIENT_SECRET) — Spotify integration will fail at runtime.");
        }
    }

    private synchronized String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedToken;
        }

        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        try {
            SpotifyTokenResponse response = restClient.post()
                    .uri("https://accounts.spotify.com/api/token")
                    .header("Authorization", "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(SpotifyTokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new SpotifyApiException(
                        "Spotify token response was empty — check client credentials", 503);
            }
            cachedToken = response.accessToken();
            tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn());
            return cachedToken;
        } catch (SpotifyApiException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new SpotifyApiException(
                    "Failed to obtain Spotify access token (HTTP " + e.getStatusCode().value() +
                    ") — check SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET", 503, e);
        }
    }

    public List<SpotifyArtist> searchArtists(String query, int limit) {
        return executeWithRetry("artist search", () -> {
            SpotifySearchResponse r = restClient.get()
                    .uri("https://api.spotify.com/v1/search?q={q}&type=artist&limit={limit}", query, limit)
                    .header("Authorization", "Bearer " + accessToken())
                    .retrieve()
                    .body(SpotifySearchResponse.class);
            return r == null || r.artists() == null || r.artists().items() == null ? List.of() : r.artists().items();
        });
    }

    public SpotifyArtist getArtist(String spotifyArtistId) {
        return executeWithRetry("get artist " + spotifyArtistId, () -> {
            SpotifyArtist artist = restClient.get()
                    .uri("https://api.spotify.com/v1/artists/{id}", spotifyArtistId)
                    .header("Authorization", "Bearer " + accessToken())
                    .retrieve()
                    .body(SpotifyArtist.class);
            if (artist == null) {
                throw new SpotifyApiException("Spotify returned empty body for artist " + spotifyArtistId, 502);
            }
            return artist;
        });
    }

    public List<SpotifyTrack> getTopTracks(String artistId) {
        return executeWithRetry("get top tracks for " + artistId, () -> {
            SpotifyTopTracksResponse r = restClient.get()
                    .uri("https://api.spotify.com/v1/artists/{id}/top-tracks?market={market}", artistId, market)
                    .header("Authorization", "Bearer " + accessToken())
                    .retrieve()
                    .body(SpotifyTopTracksResponse.class);
            return r == null || r.tracks() == null ? List.of() : r.tracks();
        });
    }

    public List<SpotifyTrack> searchTracksByArtistName(String artistName, int limit) {
        String query = "artist:\"" + artistName + "\"";
        return executeWithRetry("track search for artist " + artistName, () -> {
            SpotifyTrackSearchResponse r = restClient.get()
                    .uri("https://api.spotify.com/v1/search?q={q}&type=track&limit={limit}", query, limit)
                    .header("Authorization", "Bearer " + accessToken())
                    .retrieve()
                    .body(SpotifyTrackSearchResponse.class);
            return r == null || r.tracks() == null || r.tracks().items() == null ? List.of() : r.tracks().items();
        });
    }

    // --- Retry / error handling ---

    private <T> T executeWithRetry(String operation, Supplier<T> call) {
        return attemptCall(operation, call, true);
    }

    private <T> T attemptCall(String operation, Supplier<T> call, boolean retryOn429) {
        try {
            return call.get();
        } catch (SpotifyApiException e) {
            throw e;
        } catch (RestClientResponseException e) {
            SpotifyApiException se = toSpotifyException(operation, e);
            if (retryOn429 && se.getHttpStatus() == 429 && se.getRetryAfterMs() > 0) {
                log.warn("Spotify rate-limited on '{}'; waiting {}ms before retry", operation, se.getRetryAfterMs());
                try {
                    Thread.sleep(se.getRetryAfterMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw se;
                }
                return attemptCall(operation, call, false);
            }
            throw se;
        } catch (RestClientException e) {
            throw new SpotifyApiException("Spotify unreachable during: " + operation, 503, e);
        }
    }

    private SpotifyApiException toSpotifyException(String operation, RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 429) {
            String retryAfterHeader = e.getResponseHeaders() != null
                    ? e.getResponseHeaders().getFirst("Retry-After") : null;
            long retryAfterMs = 1000L;
            String msg = "Spotify rate limit reached during: " + operation;
            if (retryAfterHeader != null) {
                try {
                    retryAfterMs = Math.min(Long.parseLong(retryAfterHeader), 30) * 1000L;
                    msg += " (retry after " + retryAfterHeader + "s)";
                } catch (NumberFormatException ignored) {}
            }
            return new SpotifyApiException(msg, 429, retryAfterMs, e);
        }
        return new SpotifyApiException(operation + " failed: HTTP " + status, status, e);
    }

    // --- DTOs ---

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

        public String thumbnailUrl() {
            return (images != null && images.length > 0) ? images[images.length - 1].url() : null;
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
    private record SpotifyTrackPage(List<SpotifyTrack> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyTrackSearchResponse(SpotifyTrackPage tracks) {}

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
}
