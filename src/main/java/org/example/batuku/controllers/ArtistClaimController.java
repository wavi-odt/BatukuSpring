package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.ArtistClaimResponse;
import org.example.batuku.services.ArtistClaimService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/artist-claims")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class ArtistClaimController {

    private final ArtistClaimService claimService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public ArtistClaimController(ArtistClaimService claimService,
                                  JwtUserDetailsService jwtUserDetailsService) {
        this.claimService = claimService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<ArtistClaimResponse> submit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String spotifyArtistId,
            @RequestParam("selfie") MultipartFile selfie,
            @RequestParam("idDocument") MultipartFile idDocument) {

        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        ArtistClaimResponse response = claimService.submitClaim(user, spotifyArtistId, selfie, idDocument);
        return ResponseEntity.created(URI.create("/api/artist-claims/" + response.getId()))
                .body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ARTIST')")
    public List<ArtistClaimResponse> myRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return claimService.findByUser(user.getId());
    }
}
