package org.example.batuku.controllers;

import org.example.batuku.services.ArtistProfileService;
import org.example.batuku.services.AuthQuoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class PublicArtistController {

    private final ArtistProfileService artistProfileService;
    private final AuthQuoteService authQuoteService;

    public PublicArtistController(ArtistProfileService artistProfileService,
                                  AuthQuoteService authQuoteService) {
        this.artistProfileService = artistProfileService;
        this.authQuoteService = authQuoteService;
    }

    @GetMapping("/artists/hero")
    public List<ArtistProfileService.HeroArtistDto> getHeroArtists() {
        return artistProfileService.getHeroArtists();
    }

    @GetMapping("/auth-quote/{page}")
    public AuthQuoteService.AuthQuoteResponse getAuthQuote(@PathVariable String page) {
        return authQuoteService.getPublic(page);
    }
}
