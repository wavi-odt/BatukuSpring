package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.services.PlaylistLikeService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/playlist-likes")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class PlaylistLikeController {

    private final PlaylistLikeService    playlistLikeService;
    private final JwtUserDetailsService  jwtUserDetailsService;

    public PlaylistLikeController(PlaylistLikeService playlistLikeService,
                                  JwtUserDetailsService jwtUserDetailsService) {
        this.playlistLikeService   = playlistLikeService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping("/{playlistId}")
    public ResponseEntity<Void> like(@AuthenticationPrincipal UserDetails userDetails,
                                     @PathVariable Long playlistId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        playlistLikeService.like(user, playlistId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> unlike(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long playlistId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        playlistLikeService.unlike(user, playlistId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{playlistId}/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable Long playlistId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        PlaylistLikeService.LikeStatus s = playlistLikeService.status(user, playlistId);
        return ResponseEntity.ok(Map.of("liked", s.liked(), "count", s.count()));
    }
}
