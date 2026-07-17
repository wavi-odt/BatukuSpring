package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.UserDetailResponse;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class UserController {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final JwtUserDetailsService jwtUserDetailsService;

    public UserController(UserRepository userRepository,
                          FollowRepository followRepository,
                          JwtUserDetailsService jwtUserDetailsService) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping("/{id}")
    public UserDetailResponse getUser(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findById(id).orElseThrow();
        long followers = followRepository.countByFolloweeId(id);
        long following = followRepository.countByFollowerId(id);

        boolean isFollowing = false;
        if (userDetails != null) {
            User me = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
            isFollowing = followRepository.existsByFollowerIdAndFolloweeId(me.getId(), id);
        }

        return new UserDetailResponse(
                user.getId(),
                user.getName(),
                "@" + user.getUsername(),
                user.getAvatarUrl(),
                null,
                followers,
                following,
                isFollowing,
                0
        );
    }
}
