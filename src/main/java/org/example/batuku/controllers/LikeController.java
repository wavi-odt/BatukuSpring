package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.services.LikeService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class LikeController {

    private final LikeService likeService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public LikeController(LikeService likeService,
                          JwtUserDetailsService jwtUserDetailsService) {
        this.likeService = likeService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping("/{trackId}")
    public ResponseEntity<Void> like(@AuthenticationPrincipal UserDetails userDetails,
                                     @PathVariable Long trackId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        likeService.like(user, trackId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{trackId}")
    public ResponseEntity<Void> unlike(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long trackId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        likeService.unlike(user, trackId);
        return ResponseEntity.noContent().build();
    }
}
