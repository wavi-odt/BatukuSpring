package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.PendingArtistClaimAdminResponse;
import org.example.batuku.services.PendingArtistClaimService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pending-artist-claims")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPendingArtistClaimController {

    private final PendingArtistClaimService service;
    private final JwtUserDetailsService jwtUserDetailsService;

    public AdminPendingArtistClaimController(PendingArtistClaimService service,
                                              JwtUserDetailsService jwtUserDetailsService) {
        this.service = service;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping
    public List<PendingArtistClaimAdminResponse> listPending() {
        return service.listPending();
    }

    @GetMapping("/{id}")
    public PendingArtistClaimAdminResponse getDetail(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/verify")
    public PendingArtistClaimAdminResponse verify(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        User admin = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return service.verify(id, admin);
    }

    @PostMapping("/{id}/doubtful")
    public PendingArtistClaimAdminResponse markDoubtful(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        User admin = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return service.markDoubtful(id, admin);
    }
}
