package org.example.batuku.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ItunesClient {

    private final RestClient restClient;

    public ItunesClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ItunesTrack> searchTracksByArtist(String artistName, int limit) {
        ItunesSearchResponse response = restClient.get()
                .uri("https://itunes.apple.com/search?term={term}&media=music&entity=song&limit={limit}",
                        artistName, limit)
                .retrieve()
                .body(ItunesSearchResponse.class);
        return response == null ? List.of() : response.results();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItunesTrack(
            @JsonProperty("trackId") Long trackId,
            @JsonProperty("trackName") String trackName,
            @JsonProperty("artistName") String artistName,
            @JsonProperty("previewUrl") String previewUrl,
            @JsonProperty("trackTimeMillis") Integer trackTimeMillis,
            @JsonProperty("artworkUrl100") String artworkUrl,
            @JsonProperty("trackViewUrl") String trackViewUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ItunesSearchResponse(List<ItunesTrack> results) {}
}
