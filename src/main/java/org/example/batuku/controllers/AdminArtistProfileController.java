package org.example.batuku.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.example.batuku.dto.ArtistImportResponse;
import org.example.batuku.dto.ArtistSearchResult;
import org.example.batuku.services.ArtistProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/artist-profiles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminArtistProfileController {

    private final ArtistProfileService artistProfileService;

    public AdminArtistProfileController(ArtistProfileService artistProfileService) {
        this.artistProfileService = artistProfileService;
    }

    @GetMapping("/search")
    public List<ArtistSearchResult> search(@RequestParam("q") String query) {
        var artists = artistProfileService.search(query);
        var importedIds = artistProfileService.findImportedIds(
                artists.stream().map(a -> a.id()).toList()
        );
        return artists.stream()
                .map(a -> ArtistSearchResult.from(a, importedIds.contains(a.id())))
                .toList();
    }

    @PostMapping("/import")
    public ArtistImportResponse importArtist(@RequestBody @Valid ImportRequest request) {
        ArtistProfileService.ImportResult result = artistProfileService.importArtist(request.spotifyArtistId());
        return ArtistImportResponse.from(result.profile(), result.tracksImported(), result.tracksUpdated(), result.tracksSkipped());
    }

    record ImportRequest(
            @NotBlank
            @Pattern(regexp = "[A-Za-z0-9]{22}", message = "must be a valid Spotify ID (22 alphanumeric characters)")
            String spotifyArtistId
    ) {}
}
