package org.example.batuku.controllers;

import jakarta.validation.Valid;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ChangePasswordRequest;
import org.example.batuku.dto.UpdateProfileRequest;
import org.example.batuku.services.UserProfileService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class UserController {

    private final UserProfileService     userProfileService;
    private final JwtUserDetailsService  jwtUserDetailsService;

    public UserController(UserProfileService userProfileService,
                          JwtUserDetailsService jwtUserDetailsService) {
        this.userProfileService   = userProfileService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User viewer = userDetails != null
                ? jwtUserDetailsService.loadUserEntity(userDetails.getUsername()) : null;
        return ResponseEntity.ok(userProfileService.getUserDetail(id, viewer));
    }

    @PutMapping("/me/nameUsername")
    public ResponseEntity<?> updateMe(@Valid @RequestBody UpdateProfileRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(userProfileService.updateNameUsername(user, request.getName(), request.getUsername()));
    }

    @PutMapping("/me/location")
    public ResponseEntity<?> updateLocation(@RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        String location = body.getOrDefault("location", "").trim();
        return ResponseEntity.ok(userProfileService.updateLocation(user, location));
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> setPassword(@RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        String newPassword = body.getOrDefault("newPassword", "");
        userProfileService.setPassword(user, newPassword);
        return ResponseEntity.ok(Map.of("message", "Palavra-passe definida com sucesso."));
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        userProfileService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Palavra-passe alterada."));
    }

    @PostMapping("/me/marketplace-role")
    public ResponseEntity<?> setMarketplaceRole(@RequestBody Map<String, String> body,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        String role = userProfileService.setMarketplaceRole(user, body.getOrDefault("role", ""));
        return ResponseEntity.ok(Map.of("marketplaceRole", role));
    }

    @GetMapping("/{id}/recently-played")
    public ResponseEntity<List<Map<String, Object>>> getRecentlyPlayed(@PathVariable Long id) {
        return ResponseEntity.ok(userProfileService.getRecentlyPlayed(id));
    }

    @GetMapping("/{id}/top-genres")
    public ResponseEntity<List<Map<String, Object>>> getTopGenres(@PathVariable Long id) {
        return ResponseEntity.ok(userProfileService.getTopGenres(id));
    }

    @GetMapping("/me/recently-played")
    public ResponseEntity<List<Map<String, Object>>> getMyRecentlyPlayed(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(userProfileService.getMyRecentlyPlayed(user));
    }

    @GetMapping("/me/top-genres")
    public ResponseEntity<List<Map<String, Object>>> getMyTopGenres(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(userProfileService.getMyTopGenres(user));
    }
}
