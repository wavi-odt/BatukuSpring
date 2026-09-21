package org.example.batuku.controllers;

import org.example.batuku.domain.PersonalUpload;
import org.example.batuku.domain.User;
import org.example.batuku.services.PersonalUploadService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/my-uploads")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class PersonalUploadController {

    private final PersonalUploadService service;
    private final JwtUserDetailsService jwtUserDetailsService;

    public PersonalUploadController(PersonalUploadService service, JwtUserDetailsService jwtUserDetailsService) {
        this.service = service;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PersonalUploadResponse> upload(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String title,
            @RequestParam("audio") MultipartFile audio) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        PersonalUpload upload = service.upload(user, title, audio);
        return ResponseEntity.created(URI.create("/api/my-uploads/" + upload.getId()))
                .body(PersonalUploadResponse.from(upload));
    }

    @GetMapping
    public List<PersonalUploadResponse> listMine(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return service.listMine(user).stream().map(PersonalUploadResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        service.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate-link")
    public ResponseEntity<Map<String, String>> regenerateLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        PersonalUpload upload = service.regenerateShareToken(user, id);
        return ResponseEntity.ok(Map.of("shareToken", upload.getShareToken()));
    }

    record PersonalUploadResponse(Long id, String title, String audioUrl, String shareToken, LocalDateTime createdAt) {
        static PersonalUploadResponse from(PersonalUpload u) {
            return new PersonalUploadResponse(u.getId(), u.getTitle(), u.getAudioUrl(), u.getShareToken(), u.getCreatedAt());
        }
    }
}
