package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.PlaylistDetailResponse;
import org.example.batuku.dto.PlaylistResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.services.PlaylistService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public PlaylistController(PlaylistService playlistService,
                              JwtUserDetailsService jwtUserDetailsService) {
        this.playlistService = playlistService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PlaylistResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        PlaylistResponse created = playlistService.create(user, name, description, cover);
        return ResponseEntity.created(URI.create("/api/playlists/" + created.getId())).body(created);
    }

    @GetMapping("/user/{userId}")
    public List<PlaylistResponse> listByUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return playlistService.listPublicByUser(userId, user);
    }

    @GetMapping("/my")
    public List<PlaylistResponse> listMine(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return playlistService.listMine(user);
    }

    @GetMapping("/my/tracks")
    public List<TrackResponse> libraryTracks(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return playlistService.libraryTracks(user);
    }

    @GetMapping("/{id}")
    public PlaylistDetailResponse getDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return playlistService.getDetail(id, user);
    }

    @PatchMapping(value = "/{id}", consumes = "multipart/form-data")
    public PlaylistResponse update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return playlistService.update(id, user, name, description, cover);
    }

    @PatchMapping("/{id}/visibility")
    public PlaylistResponse toggleVisibility(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return playlistService.toggleVisibility(id, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        playlistService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tracks")
    public ResponseEntity<Void> addTrack(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        playlistService.addTrack(id, user, body.get("trackId"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/tracks/{trackId}")
    public ResponseEntity<Void> removeTrack(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @PathVariable Long trackId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        playlistService.removeTrack(id, user, trackId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<Void> save(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        playlistService.savePlaylist(id, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/save")
    public ResponseEntity<Void> unsave(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        playlistService.unsavePlaylist(id, user);
        return ResponseEntity.noContent().build();
    }
}
