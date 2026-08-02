package org.example.batuku.dto;

import org.example.batuku.utils.BatukuOptions;
import java.util.List;

public record ArtistOptionsResponse(
        List<String> genres,
        List<String> languages,
        List<BatukuOptions.LocationOption> locations
) {
    public static ArtistOptionsResponse defaults() {
        return new ArtistOptionsResponse(
                BatukuOptions.GENRES,
                BatukuOptions.LANGUAGES,
                BatukuOptions.LOCATIONS
        );
    }
}
