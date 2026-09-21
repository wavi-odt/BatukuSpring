package org.example.batuku.controllers;

import org.example.batuku.domain.PersonalProject;
import org.example.batuku.domain.PersonalProjectTrack;
import org.example.batuku.domain.User;
import org.example.batuku.services.PersonalProjectService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/my-projects")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class PersonalProjectController {

    private final PersonalProjectService service;
    private final JwtUserDetailsService  jwtUserDetailsService;

    public PersonalProjectController(PersonalProjectService service,
                                     JwtUserDetailsService jwtUserDetailsService) {
        this.service              = service;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    /* ─── Projetos ──────────────────────────────────────────────── */

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String name,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        PersonalProject project = service.createProject(user, name, cover);
        return ResponseEntity
                .created(URI.create("/api/my-projects/" + project.getId()))
                .body(ProjectResponse.from(project));
    }

    @GetMapping
    public List<ProjectResponse> listMine(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return service.listMine(user).stream().map(ProjectResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProjectResponse get(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ProjectResponse.from(service.getProject(user, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        service.deleteProject(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ProjectResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        PersonalProject updated = service.updateProject(user, id, name, cover);
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }

    @PostMapping("/{id}/regenerate-link")
    public ResponseEntity<ProjectResponse> regenerateLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        PersonalProject updated = service.regenerateShareToken(user, id);
        return ResponseEntity.ok(ProjectResponse.from(updated));
    }

    /* ─── Faixas ─────────────────────────────────────────────────── */

    @PostMapping(value = "/{id}/tracks", consumes = "multipart/form-data")
    public ResponseEntity<TrackResponse> addTrack(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam("audio") MultipartFile audio) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        PersonalProjectTrack track = service.addTrack(user, id, title, audio);
        return ResponseEntity
                .created(URI.create("/api/my-projects/" + id + "/tracks/" + track.getId()))
                .body(TrackResponse.from(track));
    }

    @GetMapping("/{id}/tracks")
    public List<TrackResponse> listTracks(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return service.listTracks(user, id).stream().map(TrackResponse::from).toList();
    }

    @DeleteMapping("/{id}/tracks/{trackId}")
    public ResponseEntity<Void> deleteTrack(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @PathVariable Long trackId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        service.deleteTrack(user, id, trackId);
        return ResponseEntity.noContent().build();
    }

    /* ─── Response records ──────────────────────────────────────── */

    record ProjectResponse(
            Long id,
            String name,
            String coverUrl,
            String shareToken,
            int trackCount,
            LocalDateTime createdAt) {

        static ProjectResponse from(PersonalProject p) {
            return new ProjectResponse(
                    p.getId(), p.getName(), p.getCoverUrl(),
                    p.getShareToken(), p.getTrackCount(), p.getCreatedAt());
        }
    }

    record TrackResponse(Long id, String title, String audioUrl, Integer duration, LocalDateTime createdAt) {
        static TrackResponse from(PersonalProjectTrack t) {
            return new TrackResponse(t.getId(), t.getTitle(), t.getAudioUrl(), t.getDurationSeconds(), t.getCreatedAt());
        }
    }
}
