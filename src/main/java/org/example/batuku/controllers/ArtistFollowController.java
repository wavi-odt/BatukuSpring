package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.services.ArtistFollowService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/artist-follows")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class ArtistFollowController {

    private final ArtistFollowService artistFollowService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public ArtistFollowController(ArtistFollowService artistFollowService,
                                  JwtUserDetailsService jwtUserDetailsService) {
        this.artistFollowService = artistFollowService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping("/{artistProfileId}")
    public ResponseEntity<Map<String, Object>> follow(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long artistProfileId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        long followers = artistFollowService.follow(user, artistProfileId);
        return ResponseEntity.ok(Map.of(
                "followers", followers,
                "following", true
        ));
    }

    @DeleteMapping("/{artistProfileId}")
    public ResponseEntity<Map<String, Object>> unfollow(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long artistProfileId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        long followers = artistFollowService.unfollow(user, artistProfileId);
        return ResponseEntity.ok(Map.of(
                "followers", followers,
                "following", false
        ));
    }

    @GetMapping("/{artistProfileId}/status")
    public ResponseEntity<Map<String, Object>> status(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long artistProfileId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        boolean following = artistFollowService.isFollowing(user.getId(), artistProfileId);
        long followers = artistFollowService.followerCount(artistProfileId);
        return ResponseEntity.ok(Map.of(
                "followers", followers,
                "following", following
        ));
    }
}
