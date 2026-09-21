package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.NotificationResponse;
import org.example.batuku.services.NotificationService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService    notificationService;
    private final JwtUserDetailsService  jwtUserDetailsService;

    public NotificationController(NotificationService notificationService,
                                  JwtUserDetailsService jwtUserDetailsService) {
        this.notificationService   = notificationService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping
    public List<NotificationResponse> listar(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return notificationService.listar(user.getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> contarNaoLidas(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return Map.of("count", notificationService.contarNaoLidas(user.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> marcarLida(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        notificationService.marcarLida(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> marcarTodasLidas(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        notificationService.marcarTodasLidas(user.getId());
        return ResponseEntity.ok().build();
    }
}
