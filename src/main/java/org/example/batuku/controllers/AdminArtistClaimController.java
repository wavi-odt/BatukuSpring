package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.ArtistClaimDetailResponse;
import org.example.batuku.services.ArtistClaimService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/artist-claims")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
@PreAuthorize("hasRole('ADMIN')")
public class AdminArtistClaimController {

    private final ArtistClaimService claimService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public AdminArtistClaimController(ArtistClaimService claimService,
                                       JwtUserDetailsService jwtUserDetailsService) {
        this.claimService = claimService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping
    public List<ArtistClaimDetailResponse> listPending() {
        return claimService.listPending();
    }

    @GetMapping("/{id}")
    public ArtistClaimDetailResponse getDetail(@PathVariable Long id) {
        return claimService.findById(id);
    }

    @PostMapping("/{id}/verify")
    public ArtistClaimDetailResponse verify(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        User admin = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return claimService.verify(id, admin);
    }

    @PostMapping("/{id}/doubtful")
    public ArtistClaimDetailResponse markDoubtful(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        User admin = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return claimService.markDoubtful(id, admin);
    }
}
