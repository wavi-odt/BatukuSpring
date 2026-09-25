package org.example.batuku.controllers;

import jakarta.validation.Valid;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ChangePasswordRequest;
import org.example.batuku.dto.UpdateProfileRequest;
import org.example.batuku.dto.UserDetailResponse;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.BadgeRepository;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.PlayRepository;
import org.example.batuku.repository.UserBadgeRepository;
import org.example.batuku.repository.UserPointsRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class UserController {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final PlayRepository playRepository;
    private final UserPointsRepository userPointsRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final BadgeRepository badgeRepository;
    private final JwtUserDetailsService jwtUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          FollowRepository followRepository,
                          ArtistFollowRepository artistFollowRepository,
                          ArtistProfileRepository artistProfileRepository,
                          PlayRepository playRepository,
                          UserPointsRepository userPointsRepository,
                          UserBadgeRepository userBadgeRepository,
                          BadgeRepository badgeRepository,
                          JwtUserDetailsService jwtUserDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.artistFollowRepository = artistFollowRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.playRepository = playRepository;
        this.userPointsRepository = userPointsRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.badgeRepository = badgeRepository;
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/{id}")
    public UserDetailResponse getUser(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findById(id).orElseThrow();
        Long artistProfileId = artistProfileRepository.findByUserId(id)
                .map(a -> a.getId())
                .orElse(null);

        long followers = artistProfileId != null
                ? artistFollowRepository.countByArtistProfileId(artistProfileId)
                : followRepository.countByFolloweeId(id);
        long following = followRepository.countByFollowerId(id)
                       + artistFollowRepository.countByFollowerId(id);

        boolean isFollowing = false;
        if (userDetails != null) {
            User me = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
            isFollowing = artistProfileId != null
                    ? artistFollowRepository.existsByFollowerIdAndArtistProfileId(me.getId(), artistProfileId)
                    : followRepository.existsByFollowerIdAndFolloweeId(me.getId(), id);
        }

        int points     = userPointsRepository.findByUserId(id).map(up -> up.getTotalPoints()).orElse(0);
        long rank      = userPointsRepository.findByUserId(id).isPresent() ? userPointsRepository.findRankByUserId(id) : 0L;

        var earnedBadges = userBadgeRepository.findByUserId(id);
        var earnedIds    = earnedBadges.stream()
                .map(ub -> ub.getBadge().getId())
                .collect(java.util.stream.Collectors.toSet());
        var earnedAtMap  = earnedBadges.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ub -> ub.getBadge().getId(),
                        ub -> ub.getEarnedAt() != null ? ub.getEarnedAt().toString() : ""));

        var badges = badgeRepository.findAll().stream()
                .map(b -> new UserDetailResponse.BadgeResult(
                        b.getId(), b.getName(), b.getDescription(), b.getIconUrl(),
                        b.getPointsRequired(), earnedIds.contains(b.getId()),
                        earnedAtMap.getOrDefault(b.getId(), null)))
                .toList();

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
                0,
                artistProfileId,
                points,
                rank,
                earnedIds.size(),
                badges
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

    /**
     * POST /api/users/me/marketplace-role
     * Define o papel do utilizador no marketplace (FAN ou PRODUCER).
     * Só pode ser definido uma vez — permanente.
     */
    @PostMapping("/me/marketplace-role")
    public ResponseEntity<?> setMarketplaceRole(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        if (user.getMarketplaceRole() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Papel no marketplace já definido."));
        }
        String role = body.getOrDefault("role", "").toUpperCase();
        if (!role.equals("FAN") && !role.equals("PRODUCER")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Papel inválido."));
        }
        user.setMarketplaceRole(role);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("marketplaceRole", role));
    }

    /**
     * GET /api/users/{id}/recently-played
     * Devolve as últimas faixas únicas ouvidas por qualquer utilizador (perfil público).
     */
    @GetMapping("/{id}/recently-played")
    public ResponseEntity<List<Map<String, Object>>> getRecentlyPlayed(@PathVariable Long id) {
        userRepository.findById(id).orElseThrow();
        List<Object[]> rows = playRepository.findRecentlyPlayedByUser(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("trackId",    ((Number) row[0]).longValue());
            item.put("title",      row[1]);
            item.put("coverUrl",   row[2]);
            item.put("artistName", row[3]);
            item.put("artistId",   ((Number) row[4]).longValue());
            item.put("audioUrl",   row[5]);
            item.put("source",     row[6]);
            item.put("spotifyUrl", row[7]);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/users/{id}/top-genres
     * Devolve os géneros mais ouvidos por qualquer utilizador (perfil público).
     */
    @GetMapping("/{id}/top-genres")
    public ResponseEntity<List<Map<String, Object>>> getTopGenres(@PathVariable Long id) {
        userRepository.findById(id).orElseThrow();
        List<Object[]> rows = playRepository.findTopGenresByUser(id);
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String name  = (String) row[0];
            long   plays = ((Number) row[1]).longValue();
            int    pct   = total > 0 ? (int) Math.round(plays * 100.0 / total) : 0;
            result.add(Map.of("name", name, "plays", plays, "pct", pct));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/users/me/recently-played
     * Devolve as últimas faixas únicas ouvidas pelo utilizador autenticado.
     * Formato: [{ trackId, title, coverUrl, artistName, artistId }]
     */
    @GetMapping("/me/recently-played")
    public ResponseEntity<List<Map<String, Object>>> getMyRecentlyPlayed(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        List<Object[]> rows = playRepository.findRecentlyPlayedByUser(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("trackId",    ((Number) row[0]).longValue());
            item.put("title",      row[1]);
            item.put("coverUrl",   row[2]);
            item.put("artistName", row[3]);
            item.put("artistId",   ((Number) row[4]).longValue());
            item.put("audioUrl",   row[5]);
            item.put("source",     row[6]);
            item.put("spotifyUrl", row[7]);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/users/me/top-genres
     * Devolve os géneros mais ouvidos pelo utilizador autenticado,
     * calculados a partir das suas reproduções registadas.
     * Formato: [{ name, plays, pct }]
     */
    @GetMapping("/me/top-genres")
    public ResponseEntity<List<Map<String, Object>>> getMyTopGenres(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        List<Object[]> rows = playRepository.findTopGenresByUser(user.getId());
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String name  = (String) row[0];
            long   plays = ((Number) row[1]).longValue();
            int    pct   = total > 0 ? (int) Math.round(plays * 100.0 / total) : 0;
            result.add(Map.of("name", name, "plays", plays, "pct", pct));
        }
        return ResponseEntity.ok(result);
    }
}
