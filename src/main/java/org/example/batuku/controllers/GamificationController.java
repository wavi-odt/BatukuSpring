package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.GamificationProfileResponse;
import org.example.batuku.dto.LeaderboardResponse;
import org.example.batuku.services.GamificationService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.example.batuku.dto.ChallengeResponse;
import org.example.batuku.dto.MilestoneResponse;

import java.util.List;

@RestController
@RequestMapping("/api/gamification")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class GamificationController {

    private final GamificationService    gamificationService;
    private final JwtUserDetailsService  jwtUserDetailsService;

    public GamificationController(GamificationService gamificationService,
                                  JwtUserDetailsService jwtUserDetailsService) {
        this.gamificationService   = gamificationService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    /* GET /api/gamification/me
       Perfil de gamificação do utilizador autenticado:
       pontos, nível, XP, rank global, badges ganhos. */
    @GetMapping("/me")
    public ResponseEntity<GamificationProfileResponse> meuPerfil(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.obterPerfil(user.getId()));
    }

    /* GET /api/gamification/leaderboard?limit=10
       Top utilizadores por pontos totais. */
    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderboardResponse> leaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return ResponseEntity.ok(gamificationService.obterLeaderboard(safeLimit));
    }

    /* GET /api/gamification/badges
       Lista de todos os badges disponíveis na plataforma. */
    @GetMapping("/badges")
    public ResponseEntity<List<GamificationProfileResponse.BadgeDto>> badges() {
        return ResponseEntity.ok(gamificationService.listarTodosBadges());
    }

    /* GET /api/gamification/challenges
       Desafios ativos com progresso real do utilizador autenticado. */
    @GetMapping("/challenges")
    public ResponseEntity<List<ChallengeResponse>> challenges(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.obterDesafios(user.getId()));
    }

    /* GET /api/gamification/milestones
       Estatísticas acumuladas do utilizador autenticado. */
    @GetMapping("/milestones")
    public ResponseEntity<List<MilestoneResponse>> milestones(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(gamificationService.obterMilestones(user.getId()));
    }
}
