package org.example.batuku.controllers;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.services.ArtistProfileService;
import org.example.batuku.services.SpotifyClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/artist-profiles")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class AdminArtistProfileController {

    private final ArtistProfileService artistProfileService;

    public AdminArtistProfileController(ArtistProfileService artistProfileService) {
        this.artistProfileService = artistProfileService;
    }

    @GetMapping("/search")
    public List<SpotifyClient.SpotifyArtist> search(@RequestParam("q") String query) {
        return artistProfileService.search(query);
    }

    @PostMapping("/import")
    public ArtistProfile importArtist(@RequestBody ImportRequest request) {
        return artistProfileService.importArtist(request.spotifyArtistId());
    }

    record ImportRequest(String spotifyArtistId) {}
}
