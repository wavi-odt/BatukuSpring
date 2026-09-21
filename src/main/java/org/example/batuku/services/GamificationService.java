package org.example.batuku.services;

import org.example.batuku.domain.*;
import org.example.batuku.dto.ChallengeResponse;
import org.example.batuku.dto.GamificationProfileResponse;
import org.example.batuku.dto.LeaderboardResponse;
import org.example.batuku.dto.MilestoneResponse;
import org.example.batuku.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class GamificationService {

    /* ── Pontos ganhos por ação ──────────────────────────────────── */
    private static final Map<PointTransaction.ActionType, Integer> POINTS = Map.of(
            PointTransaction.ActionType.PLAY,             10,
            PointTransaction.ActionType.LIKE,              5,
            PointTransaction.ActionType.COMMENT,          15,
            PointTransaction.ActionType.SHARE,            20,
            PointTransaction.ActionType.MISSION_COMPLETE, 50
    );

    /* ── Pontos mínimos para cada nível (índice = nível) ────────── */
    private static final int[] LEVEL_THRESHOLDS = { 0, 0, 100, 300, 600, 1000, 1500, 2000, 3000, 4000, 5000 };

    private final UserPointsRepository      userPointsRepository;
    private final PointTransactionRepository transactionRepository;
    private final BadgeRepository           badgeRepository;
    private final UserBadgeRepository       userBadgeRepository;
    private final UserRepository            userRepository;
    private final PlayRepository            playRepository;
    private final CommentRepository         commentRepository;
    private final LikeRepository            likeRepository;
    private final ArtistFollowRepository    artistFollowRepository;
    private final PlaylistRepository        playlistRepository;
    private final NotificationService       notificationService;

    public GamificationService(UserPointsRepository userPointsRepository,
                               PointTransactionRepository transactionRepository,
                               BadgeRepository badgeRepository,
                               UserBadgeRepository userBadgeRepository,
                               UserRepository userRepository,
                               PlayRepository playRepository,
                               CommentRepository commentRepository,
                               LikeRepository likeRepository,
                               ArtistFollowRepository artistFollowRepository,
                               PlaylistRepository playlistRepository,
                               NotificationService notificationService) {
        this.userPointsRepository  = userPointsRepository;
        this.transactionRepository = transactionRepository;
        this.badgeRepository       = badgeRepository;
        this.userBadgeRepository   = userBadgeRepository;
        this.userRepository        = userRepository;
        this.playRepository        = playRepository;
        this.commentRepository     = commentRepository;
        this.likeRepository        = likeRepository;
        this.artistFollowRepository = artistFollowRepository;
        this.playlistRepository    = playlistRepository;
        this.notificationService   = notificationService;
    }

    /* ── Inicialização ────────────────────────────────────────────── */

    @Transactional
    public void inicializarPontos(User user) {
        if (user.getUserRole() == User.UserRole.ADMIN) return;
        if (userPointsRepository.findByUserId(user.getId()).isPresent()) return;
        UserPoints up = new UserPoints();
        up.setUser(user);
        up.setTotalPoints(0);
        up.setLevel(1);
        up.setExperiencePoints(0);
        up.setUpdatedAt(LocalDateTime.now());
        userPointsRepository.save(up);
    }

    /* ── Adicionar pontos ─────────────────────────────────────────── */

    @Transactional
    public void adicionarPontos(User user, PointTransaction.ActionType actionType, Long referenceId) {
        int ganhos = POINTS.getOrDefault(actionType, 0);
        if (ganhos == 0) return;

        UserPoints up = userPointsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPoints novo = new UserPoints();
                    novo.setUser(user);
                    novo.setTotalPoints(0);
                    novo.setLevel(1);
                    novo.setExperiencePoints(0);
                    return novo;
                });

        int novoTotal = up.getTotalPoints() + ganhos;
        up.setTotalPoints(novoTotal);
        up.setExperiencePoints(calcularXpNivel(novoTotal));
        up.setLevel(calcularNivel(novoTotal));
        up.setUpdatedAt(LocalDateTime.now());
        userPointsRepository.save(up);

        PointTransaction tx = new PointTransaction();
        tx.setUser(user);
        tx.setActionType(actionType);
        tx.setPoints(ganhos);
        tx.setReferenceId(referenceId);
        transactionRepository.save(tx);

        verificarBadges(user, up);
    }

    /* ── Perfil de gamificação ────────────────────────────────────── */

    @Transactional
    public GamificationProfileResponse obterPerfil(Long userId) {
        UserPoints up = userPointsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));
                    inicializarPontos(user);
                    return userPointsRepository.findByUserId(userId).orElseThrow();
                });

        List<UserBadge> userBadges = userBadgeRepository.findByUserId(userId);
        long rank = userPointsRepository.findRankByUserId(userId);

        GamificationProfileResponse resp = new GamificationProfileResponse();
        resp.setTotalPoints(up.getTotalPoints());
        resp.setLevel(up.getLevel());
        resp.setExperiencePoints(up.getExperiencePoints());
        resp.setPointsToNextLevel(calcularPontosProximoNivel(up.getTotalPoints(), up.getLevel()));
        resp.setRank(rank);
        resp.setBadges(userBadges.stream().map(ub -> {
            GamificationProfileResponse.BadgeDto dto = new GamificationProfileResponse.BadgeDto();
            dto.setId(ub.getBadge().getId());
            dto.setName(ub.getBadge().getName());
            dto.setDescription(ub.getBadge().getDescription());
            dto.setIconUrl(ub.getBadge().getIconUrl());
            dto.setPointsRequired(ub.getBadge().getPointsRequired());
            dto.setEarnedAt(ub.getEarnedAt());
            return dto;
        }).toList());

        return resp;
    }

    /* ── Leaderboard ─────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public LeaderboardResponse obterLeaderboard(int limit) {
        List<UserPoints> top = userPointsRepository
                .findTopByOrderByTotalPointsDesc(PageRequest.of(0, limit));

        List<LeaderboardResponse.EntryDto> entries = new java.util.ArrayList<>();
        long currentRank = 1;
        for (int i = 0; i < top.size(); i++) {
            UserPoints up = top.get(i);

            // Empate: só avança o rank quando os pontos diminuem
            if (i > 0 && up.getTotalPoints() < top.get(i - 1).getTotalPoints()) {
                currentRank = i + 1;
            }

            int badgeCount = userBadgeRepository.findByUserId(up.getUser().getId()).size();

            LeaderboardResponse.EntryDto entry = new LeaderboardResponse.EntryDto();
            entry.setRank(currentRank);
            entry.setUserId(up.getUser().getId());
            entry.setUsername(up.getUser().getUsername());
            entry.setName(up.getUser().getName());
            entry.setAvatarUrl(up.getUser().getAvatarUrl());
            entry.setTotalPoints(up.getTotalPoints());
            entry.setLevel(up.getLevel());
            entry.setBadgeCount(badgeCount);
            entries.add(entry);
        }

        LeaderboardResponse resp = new LeaderboardResponse();
        resp.setEntries(entries);
        return resp;
    }

    /* ── Desafios com progresso real ────────────────────────────── */

    @Transactional(readOnly = true)
    public List<ChallengeResponse> obterDesafios(Long userId) {
        LocalDateTime inicioDia     = LocalDate.now().atStartOfDay();
        LocalDateTime inicioSemana  = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDate     fimSemana     = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        String expiraSemana = expiracao(fimSemana);
        String expiraHoje   = expiracao(LocalDate.now());

        // 1. Maratonista — 60 minutos hoje
        long msHoje      = playRepository.sumDurationPlayedSince(userId, inicioDia);
        int  minutosHoje = (int) Math.min(60, msHoje / 60_000);

        // 2. Comentador — 3 comentários esta semana
        int comentarios = (int) Math.min(3, commentRepository.countByUserIdAndCreatedAtAfter(userId, inicioSemana));

        // 3. Descobridor Semanal — 5 artistas distintos esta semana
        int artistas = (int) Math.min(5, playRepository.countDistinctArtistsSince(userId, inicioSemana));

        // 4. Seguidor Ativo — 2 novos follows esta semana
        int follows = (int) Math.min(2, artistFollowRepository.countByFollowerIdAndCreatedAtAfter(userId, inicioSemana));

        // 5. Streak — dias consecutivos (últimos 30 dias)
        List<java.sql.Date> datas = playRepository.findDistinctPlayDatesSince(userId, LocalDateTime.now().minusDays(31));
        int streak = calcularStreak(datas);
        int streakTotal = 14;

        // 6. Colecionador — 5 likes esta semana
        int likes = (int) Math.min(5, likeRepository.countByUserIdAndCreatedAtAfter(userId, inicioSemana));

        List<ChallengeResponse> lista = new java.util.ArrayList<>();
        lista.add(desafio("ch-marathon",  "🎧", "Maratonista",         "Ouve 60 minutos de música hoje",           minutosHoje, 60,         200, expiraHoje,   minutosHoje >= 60));
        lista.add(desafio("ch-comment",   "💬", "Comentador",          "Deixa 3 comentários esta semana",          comentarios, 3,          100, expiraSemana, comentarios >= 3));
        lista.add(desafio("ch-discover",  "🧭", "Descobridor Semanal", "Ouve faixas de 5 artistas esta semana",   artistas,    5,          150, expiraSemana, artistas >= 5));
        lista.add(desafio("ch-follow",    "❤️",  "Seguidor Ativo",      "Segue 2 novos artistas esta semana",      follows,     2,          80,  expiraSemana, follows >= 2));
        lista.add(desafio("ch-streak",    "🔥", "Streak " + streakTotal, "Mantém o streak por " + streakTotal + " dias seguidos", Math.min(streak, streakTotal), streakTotal, 180, null, streak >= streakTotal));
        lista.add(desafio("ch-collector", "⭐", "Colecionador",        "Dá like em 5 faixas esta semana",         likes,       5,          120, expiraSemana, likes >= 5));
        return lista;
    }

    /* ── Milestones acumulados ───────────────────────────────────── */

    @Transactional(readOnly = true)
    public List<MilestoneResponse> obterMilestones(Long userId) {
        long msTotal     = playRepository.sumTotalDurationByUser(userId);
        long horasTotal  = msTotal / 3_600_000;
        long faixasUnicas = playRepository.countDistinctTracksByUser(userId);
        long artistasDesc = playRepository.countDistinctArtistsByUser(userId);
        long playlists   = playlistRepository.countByUserIdAndIsSystemGeneratedFalse(userId);
        long comentarios = commentRepository.countByUserId(userId);
        long likes       = likeRepository.countByUserId(userId);

        return List.of(
            new MilestoneResponse("Horas ouvidas",        horasTotal,   "h"),
            new MilestoneResponse("Faixas únicas",        faixasUnicas, ""),
            new MilestoneResponse("Artistas descobertos", artistasDesc, ""),
            new MilestoneResponse("Playlists criadas",    playlists,    ""),
            new MilestoneResponse("Comentários dados",    comentarios,  ""),
            new MilestoneResponse("Likes dados",          likes,        "")
        );
    }

    /* ── Todos os badges disponíveis ────────────────────────────── */

    @Transactional(readOnly = true)
    public List<GamificationProfileResponse.BadgeDto> listarTodosBadges() {
        return badgeRepository.findAll().stream().map(b -> {
            GamificationProfileResponse.BadgeDto dto = new GamificationProfileResponse.BadgeDto();
            dto.setId(b.getId());
            dto.setName(b.getName());
            dto.setDescription(b.getDescription());
            dto.setIconUrl(b.getIconUrl());
            dto.setPointsRequired(b.getPointsRequired());
            dto.setEarnedAt(null);
            return dto;
        }).toList();
    }

    /* ── Lógica interna ──────────────────────────────────────────── */

    private void verificarBadges(User user, UserPoints up) {
        List<Badge> elegíveis = badgeRepository.findByPointsRequiredLessThanEqual(up.getTotalPoints());
        for (Badge badge : elegíveis) {
            if (!userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
                UserBadge ub = new UserBadge();
                ub.setUser(user);
                ub.setBadge(badge);
                userBadgeRepository.save(ub);
                notificationService.notify(user, Notification.NotificationType.BADGE, badge.getId(),
                        "Conquistaste o badge \"" + badge.getName() + "\"!");
            }
        }
    }

    private int calcularNivel(int totalPoints) {
        int nivel = 1;
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 2; i--) {
            if (totalPoints >= LEVEL_THRESHOLDS[i]) {
                nivel = i;
                break;
            }
        }
        return nivel;
    }

    private int calcularXpNivel(int totalPoints) {
        int nivel = calcularNivel(totalPoints);
        int baseDoNivel = LEVEL_THRESHOLDS[Math.min(nivel, LEVEL_THRESHOLDS.length - 1)];
        return totalPoints - baseDoNivel;
    }

    private int calcularPontosProximoNivel(int totalPoints, int nivel) {
        if (nivel >= LEVEL_THRESHOLDS.length - 1) return 0;
        return LEVEL_THRESHOLDS[nivel + 1] - totalPoints;
    }

    private ChallengeResponse desafio(String id, String icon, String title, String desc,
                                      int progress, int total, int xp, String expires, boolean completed) {
        ChallengeResponse c = new ChallengeResponse();
        c.setId(id);
        c.setIcon(icon);
        c.setTitle(title);
        c.setDesc(desc);
        c.setProgress(progress);
        c.setTotal(total);
        c.setXp(xp);
        c.setExpires(expires);
        c.setCompleted(completed);
        return c;
    }

    private int calcularStreak(List<java.sql.Date> datas) {
        if (datas.isEmpty()) return 0;
        LocalDate hoje = LocalDate.now();
        LocalDate primeiro = datas.get(0).toLocalDate();
        // Streak só conta se jogou hoje ou ontem (não quebrou ainda hoje)
        if (!primeiro.equals(hoje) && !primeiro.equals(hoje.minusDays(1))) return 0;
        int streak = 0;
        for (java.sql.Date d : datas) {
            if (d.toLocalDate().equals(hoje.minusDays(streak))) streak++;
            else break;
        }
        return streak;
    }

    private String expiracao(LocalDate fim) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fim);
        if (dias <= 0) return "hoje";
        if (dias == 1) return "amanhã";
        return "em " + dias + " dias";
    }
}
