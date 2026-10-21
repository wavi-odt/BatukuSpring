package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.services.UserProfileService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class MediaController {

    private final UserProfileService    userProfileService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public MediaController(UserProfileService userProfileService,
                           JwtUserDetailsService jwtUserDetailsService) {
        this.userProfileService   = userProfileService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PutMapping(value = "/avatar", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Não autenticado"));
        }
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(userProfileService.updateAvatar(user, file));
    }

    @PostMapping(value = "/audio", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userProfileService.uploadAudio(file));
    }
}
