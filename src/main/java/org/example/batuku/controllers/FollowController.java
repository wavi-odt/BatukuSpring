package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.FollowUserResponse;
import org.example.batuku.services.FollowService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follows")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class FollowController {

    private final FollowService followService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public FollowController(FollowService followService,
                            JwtUserDetailsService jwtUserDetailsService) {
        this.followService = followService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Void> follow(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long userId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        followService.follow(user, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfollow(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Long userId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        followService.unfollow(user, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/followers/{userId}")
    public List<FollowUserResponse> followers(@PathVariable Long userId) {
        return followService.followers(userId);
    }

    @GetMapping("/following/{userId}")
    public List<FollowUserResponse> following(@PathVariable Long userId) {
        return followService.following(userId);
    }
}
