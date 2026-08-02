package org.example.batuku.controllers;

import jakarta.validation.Valid;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ChangePasswordRequest;
import org.example.batuku.dto.UpdateProfileRequest;
import org.example.batuku.dto.UserDetailResponse;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class UserController {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final JwtUserDetailsService jwtUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          FollowRepository followRepository,
                          JwtUserDetailsService jwtUserDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.passwordEncoder = passwordEncoder;
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
                user.getBio(),
                user.getLocation(),
                followers,
                following,
                isFollowing,
                0
        );
    }

    /**
     * PUT /api/users/me
     * Atualiza nome e username do utilizador autenticado.
     */
    @PutMapping("/me/nameUsername")
    public ResponseEntity<?> updateMe(@Valid @RequestBody UpdateProfileRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());

        String newUsername = request.getUsername().trim();
        if (!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Este username já está em uso."));
        }

        user.setName(request.getName().trim());
        user.setUsername(newUsername);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * PUT /api/users/me/password
     * Altera a palavra-passe do utilizador autenticado.
     */
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());

        if (user.getPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Palavra-passe atual incorreta."));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Palavra-passe alterada."));
    }
}
