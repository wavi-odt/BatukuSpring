package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.FollowUserResponse;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.services.FollowService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follows")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class FollowController {

    private final FollowService followService;
    private final FollowRepository followRepository;
    private final JwtUserDetailsService jwtUserDetailsService;

    public FollowController(FollowService followService,
                            FollowRepository followRepository,
                            JwtUserDetailsService jwtUserDetailsService) {
        this.followService = followService;
        this.followRepository = followRepository;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Map<String, Long>> follow(@AuthenticationPrincipal UserDetails userDetails,
                                                    @PathVariable Long userId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        followService.follow(user, userId);
        long followers = followRepository.countByFolloweeId(userId);
        return ResponseEntity.ok(Map.of("followers", followers));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Long>> unfollow(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable Long userId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        followService.unfollow(user, userId);
        long followers = followRepository.countByFolloweeId(userId);
        return ResponseEntity.ok(Map.of("followers", followers));
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
