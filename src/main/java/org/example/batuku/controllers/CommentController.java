package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.CommentResponse;
import org.example.batuku.services.CommentService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class CommentController {

    private final CommentService        commentService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public CommentController(CommentService commentService,
                             JwtUserDetailsService jwtUserDetailsService) {
        this.commentService        = commentService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping("/track/{trackId}")
    public List<CommentResponse> listByTrack(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long trackId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return commentService.listByTrack(trackId, user.getId());
    }

    @PostMapping("/track/{trackId}")
    public ResponseEntity<CommentResponse> addToTrack(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long trackId,
            @RequestBody Map<String, String> body) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(commentService.addToTrack(user, trackId, body.get("content")));
    }

    @GetMapping("/release/{albumId}")
    public List<CommentResponse> listByAlbum(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long albumId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return commentService.listByAlbum(albumId, user.getId());
    }

    @PostMapping("/release/{albumId}")
    public ResponseEntity<CommentResponse> addToAlbum(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long albumId,
            @RequestBody Map<String, String> body) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(commentService.addToAlbum(user, albumId, body.get("content")));
    }

    @GetMapping("/artist")
    public List<CommentResponse> listByArtist(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return commentService.listByArtist(user);
    }

    @PostMapping("/{commentId}/reply")
    public ResponseEntity<CommentResponse> addReply(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId,
            @RequestBody Map<String, String> body) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(commentService.addReply(user, commentId, body.get("content")));
    }

    @PatchMapping("/{commentId}/pin")
    public ResponseEntity<Void> togglePin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId,
            @RequestBody Map<String, Boolean> body) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        commentService.togglePin(user, commentId, Boolean.TRUE.equals(body.get("pinned")));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        commentService.delete(user, commentId);
        return ResponseEntity.noContent().build();
    }
}
