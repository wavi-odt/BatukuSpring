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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GamificationService {

    private static final Logger log = LoggerFactory.getLogger(GamificationService.class);

    /* ── Pontos ganhos por ação ──────────────────────────────────── */
    private static final Map<PointTransaction.ActionType, Integer> POINTS = Map.of(
            PointTransaction.ActionType.PLAY,             10,
            PointTransaction.ActionType.LIKE,              5,
            PointTransaction.ActionType.COMMENT,          15,
            PointTransaction.ActionType.SHARE,            20,
            PointTransaction.ActionType.FOLLOW,            5,
            PointTransaction.ActionType.MISSION_COMPLETE, 50
    );

    /* ── Pontos mínimos para cada nível (índice = nível) ────────── */
    private static final int[] LEVEL_THRESHOLDS = { 0, 0, 100, 300, 600, 1000, 1500, 2000, 3000, 4000, 5000 };

    private final UserPointsRepository         userPointsRepository;
    private final PointTransactionRepository   transactionRepository;
    private final BadgeRepository              badgeRepository;
    private final UserBadgeRepository          userBadgeRepository;
    private final UserRepository               userRepository;
    private final PlayRepository               playRepository;
    private final CommentRepository            commentRepository;
    private final LikeRepository               likeRepository;
    private final ArtistFollowRepository       artistFollowRepository;
    private final PlaylistRepository           playlistRepository;
    private final NotificationService          notificationService;
    private final UserChallengeRewardRepository challengeRewardRepository;
    private final org.example.batuku.repository.PlaylistLikeRepository playlistLikeRepository;

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
                               NotificationService notificationService,
                               UserChallengeRewardRepository challengeRewardRepository,
                               org.example.batuku.repository.PlaylistLikeRepository playlistLikeRepository) {
        this.userPointsRepository    = userPointsRepository;
        this.transactionRepository   = transactionRepository;
        this.badgeRepository         = badgeRepository;
        this.userBadgeRepository     = userBadgeRepository;
        this.userRepository          = userRepository;
        this.playRepository          = playRepository;
        this.commentRepository       = commentRepository;
        this.likeRepository          = likeRepository;
        this.artistFollowRepository  = artistFollowRepository;
        this.playlistRepository      = playlistRepository;
        this.notificationService     = notificationService;
        this.challengeRewardRepository = challengeRewardRepository;
        this.playlistLikeRepository  = playlistLikeRepository;
    }

    /* ── Inicialização ────────────────────────────────────────────── */

    @Transactional
    public void removerDadosUtilizador(User user) {
        userPointsRepository.findByUserId(user.getId()).ifPresent(userPointsRepository::delete);
    }

    @Transactional
    public void inicializarPontos(User user) {
        if (user.getUserRole() != User.UserRole.FAN) return;
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
        adicionarPontos(user, actionType, POINTS.getOrDefault(actionType, 0), referenceId);
    }

    @Transactional
    public void adicionarPontos(User user, PointTransaction.ActionType actionType, int ganhos, Long referenceId) {
        if (ganhos == 0) return;
        if (user.getUserRole() != User.UserRole.FAN) return;

        UserPoints up = userPointsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPoints novo = new UserPoints();
                    novo.setUser(user);
                    novo.setTotalPoints(0);
                    novo.setLevel(1);
                    novo.setExperiencePoints(0);
                    return novo;
                });

        int nivelAnterior = up.getLevel();
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

        if (up.getLevel() > nivelAnterior) {
            notificationService.notify(user, Notification.NotificationType.LEVEL_UP, null,
                    "Subiste para o Nível " + up.getLevel() + "! 🏆");
        }

        verificarBadges(user, up);
        if (actionType != PointTransaction.ActionType.MISSION_COMPLETE) {
            verificarDesafios(user);
        }
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

        // Progress labels para badges ainda não ganhos
        Set<Long> earnedIds = userBadges.stream().map(ub -> ub.getBadge().getId())
                .collect(java.util.stream.Collectors.toSet());
        List<Badge> porGanhar = badgeRepository.findAll().stream()
                .filter(b -> !earnedIds.contains(b.getId())).toList();

        if (!porGanhar.isEmpty()) {
            List<java.sql.Date> dates = playRepository.findDistinctPlayDatesSince(userId, LocalDateTime.now().minusDays(35));
            int  streak        = calcularStreak(dates);
            long earlyDays     = playRepository.countDistinctEarlyMorningDays(userId, 7);
            long artists       = playRepository.countDistinctArtistsByUser(userId);
            long follows       = artistFollowRepository.countByFollowerId(userId);
            long playlists     = playlistRepository.countByUserIdAndIsSystemGeneratedFalse(userId);
            long hoursOuvidas     = playRepository.sumTotalDurationByUser(userId) / 3_600_000;
            long maxPlaylistLikes = playlistLikeRepository.maxLikesByUserPlaylists(userId);
            int  level            = up.getLevel();

            java.util.Map<Long, String> labels = new java.util.HashMap<>();
            for (Badge badge : porGanhar) {
                String label = switch (badge.getName()) {
                    case "Madrugador"    -> earlyDays       + " / 10 dias";
                    case "Streak 12"     -> streak          + " / 12 dias";
                    case "Explorador"    -> artists         + " / 50 artistas";
                    case "Apoiante"      -> follows         + " / 20 artistas";
                    case "Curador"       -> maxPlaylistLikes + " / 100 likes";
                    case "Top 100"       -> rank > 0 ? "Rank #" + rank : "Sem rank ainda";
                    case "Fã Dedicado"   -> hoursOuvidas    + "h / 100h";
                    case "Streak 30"     -> streak          + " / 30 dias";
                    case "Influenciador" -> maxPlaylistLikes + " / 1000 likes";
                    case "Lenda"         -> "Nível " + level + " / 10";
                    default -> null;
                };
                if (label != null) labels.put(badge.getId(), label);
            }
            resp.setBadgeProgressLabels(labels);
        }

        return resp;
    }

    /* ── Leaderboard ─────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public LeaderboardResponse obterLeaderboard(int limit) {
        // Buscar mais entradas do que o limite para compensar os artistas filtrados
        List<UserPoints> all = userPointsRepository
                .findTopByOrderByTotalPointsDesc(PageRequest.of(0, limit * 3));

        List<LeaderboardResponse.EntryDto> entries = new java.util.ArrayList<>();
        long currentRank = 1;
        int  fanIndex    = 0;

        for (UserPoints up : all) {
            if (entries.size() >= limit) break;
            if (up.getUser().getUserRole() == User.UserRole.ARTIST) continue;

            if (fanIndex > 0 && up.getTotalPoints() < getPreviousFanPoints(entries)) {
                currentRank = entries.size() + 1;
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
            fanIndex++;
        }

        LeaderboardResponse resp = new LeaderboardResponse();
        resp.setEntries(entries);
        return resp;
    }

    private int getPreviousFanPoints(List<LeaderboardResponse.EntryDto> entries) {
        if (entries.isEmpty()) return Integer.MAX_VALUE;
        return entries.get(entries.size() - 1).getTotalPoints();
    }

    /* ── Desafios rotativos ─────────────────────────────────────── */

    private static final int TOTAL_SETS = 3;

    @Transactional
    public List<ChallengeResponse> obterDesafios(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));
        int setOffset = user.getChallengeSetOffset();
        int setIndex  = setOffset % TOTAL_SETS;
        List<ChallengeResponse> desafios = construirSet(userId, setIndex);
        autoRewardCompleted(user, desafios, setOffset);
        return desafios;
    }

    private static final int ROTATION_BONUS_XP = 500;

    @Transactional
    public List<ChallengeResponse> avancarDesafios(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));
        int setIndex  = user.getChallengeSetOffset() % TOTAL_SETS;
        List<ChallengeResponse> atual = construirSet(userId, setIndex);
        boolean todosCompletos = atual.stream().allMatch(ChallengeResponse::isCompleted);
        if (!todosCompletos) return null;

        int novoOffset = user.getChallengeSetOffset() + 1;
        user.setChallengeSetOffset(novoOffset);
        userRepository.save(user);

        if (novoOffset % TOTAL_SETS == 0) {
            adicionarPontos(user, PointTransaction.ActionType.MISSION_COMPLETE, ROTATION_BONUS_XP, null);
            notificationService.notify(user, Notification.NotificationType.CHALLENGE_COMPLETED, null,
                    "Completaste todos os conjuntos de desafios! Bónus de +" + ROTATION_BONUS_XP + " pts. Começa de novo!");
        }

        return construirSet(userId, novoOffset % TOTAL_SETS);
    }

    private void autoRewardCompleted(User user, List<ChallengeResponse> desafios, int setOffset) {
        int setIndex = setOffset % TOTAL_SETS;
        Set<String> jaRecompensados = challengeRewardRepository.findRewardedChallengeIds(user.getId(), setOffset);

        boolean houveConclusaoNova = false;
        for (ChallengeResponse ch : desafios) {
            if (ch.isCompleted() && !jaRecompensados.contains(ch.getId())) {
                adicionarPontos(user, PointTransaction.ActionType.MISSION_COMPLETE, ch.getXp(), null);
                challengeRewardRepository.save(new UserChallengeReward(user, ch.getId(), setOffset));
                jaRecompensados.add(ch.getId());
                houveConclusaoNova = true;
                notificationService.notify(user, Notification.NotificationType.CHALLENGE_COMPLETED, null,
                        "Desafio \"" + ch.getTitle() + "\" concluído! +" + ch.getXp() + " pts 🎯");
                log.info("Challenge '{}' (set {}) rewarded: +{} pts to user {}", ch.getId(), setOffset, ch.getXp(), user.getId());
            }
        }

        if (houveConclusaoNova) {
            long totalRecompensados = challengeRewardRepository.countByUserIdAndSetOffset(user.getId(), setOffset);
            if (totalRecompensados >= desafios.size()) {
                int totalXp = desafios.stream().mapToInt(ChallengeResponse::getXp).sum();
                notificationService.notify(user, Notification.NotificationType.CHALLENGE_COMPLETED, null,
                        "Completaste todos os desafios do conjunto " + (setIndex + 1) + "! +" + totalXp + " pts");
            }
        }
    }

    private List<ChallengeResponse> construirSet(Long userId, int setIndex) {
        LocalDateTime inicioDia    = LocalDate.now().atStartOfDay();
        LocalDateTime inicioSemana = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDate     fimSemana    = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        String expiraSemana = expiracao(fimSemana);
        String expiraHoje   = expiracao(LocalDate.now());

        long msHoje    = playRepository.sumDurationPlayedSince(userId, inicioDia);
        int  minHoje   = (int)(msHoje / 60_000);
        int  comentSem = (int) commentRepository.countByUserIdAndCreatedAtAfter(userId, inicioSemana);
        int  comentHoje= (int) commentRepository.countByUserIdAndCreatedAtAfter(userId, inicioDia);
        int  artSem    = (int) playRepository.countDistinctArtistsSince(userId, inicioSemana);
        int  artHoje   = (int) playRepository.countDistinctArtistsSince(userId, inicioDia);
        int  follows   = (int) artistFollowRepository.countByFollowerIdAndCreatedAtAfter(userId, inicioSemana);
        int  likesSem  = (int) likeRepository.countByUserIdAndCreatedAtAfter(userId, inicioSemana);
        List<java.sql.Date> datas = playRepository.findDistinctPlayDatesSince(userId, LocalDateTime.now().minusDays(31));
        int streak = calcularStreak(datas);

        List<ChallengeResponse> lista = new java.util.ArrayList<>();
        switch (setIndex) {
            case 0 -> {
                lista.add(desafio("ch-marathon",  "🎧", "Maratonista",         "Ouve 60 minutos de música hoje",            cap(minHoje,60),  60,  200, expiraHoje,   minHoje  >= 60,  setIndex));
                lista.add(desafio("ch-comment",   "💬", "Comentador",          "Deixa 3 comentários esta semana",           cap(comentSem,3), 3,   100, expiraSemana, comentSem>= 3,   setIndex));
                lista.add(desafio("ch-discover",  "🧭", "Descobridor Semanal", "Ouve faixas de 5 artistas esta semana",    cap(artSem,5),    5,   150, expiraSemana, artSem   >= 5,   setIndex));
                lista.add(desafio("ch-follow",    "❤️",  "Seguidor Ativo",      "Segue 2 novos artistas esta semana",       cap(follows,2),   2,   80,  expiraSemana, follows  >= 2,   setIndex));
                lista.add(desafio("ch-streak7",   "🔥", "Streak 7",            "Mantém o streak por 7 dias seguidos",      cap(streak,7),    7,   180, null,         streak   >= 7,   setIndex));
                lista.add(desafio("ch-collector", "⭐", "Colecionador",        "Dá like em 5 faixas esta semana",          cap(likesSem,5),  5,   120, expiraSemana, likesSem >= 5,   setIndex));
            }
            case 1 -> {
                lista.add(desafio("ch-vip",       "🎧", "Ouvinte VIP",         "Ouve 90 minutos de música hoje",            cap(minHoje,90),  90,  280, expiraHoje,   minHoje  >= 90,  setIndex));
                lista.add(desafio("ch-critic",    "💬", "Crítico Cultural",    "Deixa 5 comentários esta semana",           cap(comentSem,5), 5,   160, expiraSemana, comentSem>= 5,   setIndex));
                lista.add(desafio("ch-explorer",  "🧭", "Explorador Total",    "Ouve faixas de 8 artistas esta semana",    cap(artSem,8),    8,   210, expiraSemana, artSem   >= 8,   setIndex));
                lista.add(desafio("ch-ambassador","❤️",  "Embaixador",          "Segue 3 novos artistas esta semana",       cap(follows,3),   3,   120, expiraSemana, follows  >= 3,   setIndex));
                lista.add(desafio("ch-streak14",  "🔥", "Streak 14",           "Mantém o streak por 14 dias seguidos",     cap(streak,14),   14,  350, null,         streak   >= 14,  setIndex));
                lista.add(desafio("ch-superfan",  "⭐", "Super Fã",            "Dá like em 10 faixas esta semana",         cap(likesSem,10), 10,  200, expiraSemana, likesSem >= 10,  setIndex));
            }
            default -> {
                lista.add(desafio("ch-ultra",     "🎧", "Maratonista Plus",    "Ouve 2 horas de música hoje",               cap(minHoje,120), 120, 400, expiraHoje,   minHoje  >= 120, setIndex));
                lista.add(desafio("ch-daily-com", "💬", "Comentador Diário",   "Deixa 1 comentário hoje",                   cap(comentHoje,1),1,   80,  expiraHoje,   comentHoje>= 1,  setIndex));
                lista.add(desafio("ch-daily-art", "🧭", "Curioso do Dia",      "Ouve faixas de 3 artistas hoje",            cap(artHoje,3),   3,   130, expiraHoje,   artHoje  >= 3,   setIndex));
                lista.add(desafio("ch-connector", "❤️",  "Conector",            "Segue 4 novos artistas esta semana",       cap(follows,4),   4,   160, expiraSemana, follows  >= 4,   setIndex));
                lista.add(desafio("ch-streak21",  "🔥", "Streak 21",           "Mantém o streak por 21 dias seguidos",     cap(streak,21),   21,  500, null,         streak   >= 21,  setIndex));
                lista.add(desafio("ch-likefest",  "⭐", "Like Fest",           "Dá like em 8 faixas esta semana",          cap(likesSem,8),  8,   160, expiraSemana, likesSem >= 8,   setIndex));
            }
        }
        return lista;
    }

    private static int cap(int value, int max) { return Math.min(value, max); }

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

    @Transactional
    public void checkBadgesForUser(User user) {
        userPointsRepository.findByUserId(user.getId())
                .ifPresent(up -> verificarBadges(user, up));
    }

    private void verificarDesafios(User user) {
        int setOffset = user.getChallengeSetOffset();
        List<ChallengeResponse> desafios = construirSet(user.getId(), setOffset % TOTAL_SETS);
        autoRewardCompleted(user, desafios, setOffset);
    }

    private void verificarBadges(User user, UserPoints up) {
        List<Badge> todos = badgeRepository.findAll();
        Set<Long> jaGanhos = userBadgeRepository.findByUserId(user.getId())
                .stream().map(ub -> ub.getBadge().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<Badge> porGanhar = todos.stream().filter(b -> !jaGanhos.contains(b.getId())).toList();
        if (porGanhar.isEmpty()) return;

        Long uid = user.getId();
        List<java.sql.Date> dates = playRepository.findDistinctPlayDatesSince(uid, LocalDateTime.now().minusDays(35));
        int  streak        = calcularStreak(dates);
        long earlyDays     = playRepository.countDistinctEarlyMorningDays(uid, 7);
        long artists       = playRepository.countDistinctArtistsByUser(uid);
        long follows       = artistFollowRepository.countByFollowerId(uid);
        long playlists     = playlistRepository.countByUserIdAndIsSystemGeneratedFalse(uid);
        long rank          = userPointsRepository.findRankByUserId(uid);
        long hoursOuvidas      = playRepository.sumTotalDurationByUser(uid) / 3_600_000;
        long maxPlaylistLikes  = playlistLikeRepository.maxLikesByUserPlaylists(uid);
        int  level             = up.getLevel();

        for (Badge badge : porGanhar) {
            boolean earned = switch (badge.getName()) {
                case "Madrugador"    -> earlyDays       >= 10;
                case "Streak 12"     -> streak          >= 12;
                case "Explorador"    -> artists         >= 50;
                case "Apoiante"      -> follows         >= 20;
                case "Curador"       -> maxPlaylistLikes >= 100;
                case "Top 100"       -> rank > 0 && rank <= 100;
                case "Fã Dedicado"   -> hoursOuvidas    >= 100;
                case "Streak 30"     -> streak          >= 30;
                case "Influenciador" -> maxPlaylistLikes >= 1000;
                case "Lenda"         -> level           >= 10;
                default -> false;
            };
            if (earned) {
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
                                      int progress, int total, int xp, String expires, boolean completed, int setIndex) {
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
        c.setSetIndex(setIndex);
        c.setTotalSets(TOTAL_SETS);
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
