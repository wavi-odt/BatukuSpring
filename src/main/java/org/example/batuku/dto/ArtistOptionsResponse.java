package org.example.batuku.dto;

import java.util.List;

public record ArtistOptionsResponse(
        List<String> genres,
        List<String> languages,
        List<LocationDto> locations
) {
    public record LocationDto(String value, String group) {}
}
