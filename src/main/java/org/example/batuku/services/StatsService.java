package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Play;
import org.example.batuku.domain.PointTransaction;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ForecastResponse;
import org.example.batuku.dto.StatsResponse;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.CommentRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.PlayRepository;
import org.example.batuku.repository.TrackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StatsService {

    private final ArtistProfileRepository artistProfileRepository;
    private final PlayRepository          playRepository;
    private final LikeRepository          likeRepository;
    private final ArtistFollowRepository  artistFollowRepository;
    private final TrackRepository         trackRepository;
    private final CommentRepository       commentRepository;
    private final GamificationService     gamificationService;
    private final ForecastService         forecastService;
    private final GroqClient              groqClient;

    public StatsService(ArtistProfileRepository artistProfileRepository,
                        PlayRepository playRepository,
                        LikeRepository likeRepository,
                        ArtistFollowRepository artistFollowRepository,
                        TrackRepository trackRepository,
                        CommentRepository commentRepository,
                        GamificationService gamificationService,
                        ForecastService forecastService,
                        GroqClient groqClient) {
        this.artistProfileRepository = artistProfileRepository;
        this.playRepository          = playRepository;
        this.likeRepository          = likeRepository;
        this.artistFollowRepository  = artistFollowRepository;
        this.trackRepository         = trackRepository;
        this.commentRepository       = commentRepository;
        this.gamificationService     = gamificationService;
        this.forecastService         = forecastService;
        this.groqClient              = groqClient;
    }

    /** Devolve o ArtistProfile do utilizador, se existir. */
    @Transactional(readOnly = true)
    public Optional<ArtistProfile> findProfileByUserId(Long userId) {
        return artistProfileRepository.findByUserId(userId);
    }

    /** GET /api/stats/me */
    @Transactional(readOnly = true)
    public StatsResponse getStats(Long artistId, String period) {
        int days = switch (period) {
            case "7d"  -> 7;
            case "90d" -> 90;
            default    -> 30;
        };

        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime since     = now.minusDays(days);
        LocalDateTime prevSince = since.minusDays(days);

        long plays     = playRepository.countByTrackArtistProfileIdAndPlayedAtAfter(artistId, since);
        long prevPlays = playRepository.countByTrackArtistProfileIdAndPlayedAtBetween(artistId, prevSince, since);

        long likes     = likeRepository.countByTrackArtistProfileIdAndCreatedAtAfter(artistId, since);
        long prevLikes = likeRepository.countByTrackArtistProfileIdAndCreatedAtBetween(artistId, prevSince, since);

        long totalFollowers = artistFollowRepository.countByArtistProfileId(artistId);
        long newFollowers   = artistFollowRepository.countByArtistProfileIdAndCreatedAtAfter(artistId, since);
        long prevFollowers  = artistFollowRepository.countByArtistProfileIdAndCreatedAtBetween(artistId, prevSince, since);

        long comments     = commentRepository.countByArtistProfileAndCreatedAtAfter(artistId, since);
        long prevComments = commentRepository.countByArtistProfileAndCreatedAtBetween(artistId, prevSince, since);

        Map<String, StatsResponse.KpiValue> kpis = new LinkedHashMap<>();
        kpis.put("plays",     new StatsResponse.KpiValue(plays,          delta(plays,          prevPlays),     "Reproduções"));
        kpis.put("likes",     new StatsResponse.KpiValue(likes,          delta(likes,          prevLikes),     "Likes"));
        kpis.put("comments",  new StatsResponse.KpiValue(comments,       delta(comments,       prevComments),  "Comentários"));
        kpis.put("followers", new StatsResponse.KpiValue(totalFollowers, delta(newFollowers,   prevFollowers), "Seguidores"));

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        List<Object[]> rawDaily = playRepository.findDailyPlaysByArtist(artistId, since);
        Map<String, Long> byDay = new LinkedHashMap<>();
        for (Object[] row : rawDaily) byDay.put(row[0].toString(), ((Number) row[1]).longValue());
        List<StatsResponse.DayCount> dailyPlays = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String d = now.minusDays(i).toLocalDate().format(fmt);
            dailyPlays.add(new StatsResponse.DayCount(d, byDay.getOrDefault(d, 0L)));
        }

        List<Object[]> rawDailyLikes = likeRepository.findDailyLikesByArtist(artistId, since);
        Map<String, Long> likesByDay = new LinkedHashMap<>();
        for (Object[] row : rawDailyLikes) likesByDay.put(row[0].toString(), ((Number) row[1]).longValue());
        List<StatsResponse.DayLike> dailyLikes = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String d = now.minusDays(i).toLocalDate().format(fmt);
            dailyLikes.add(new StatsResponse.DayLike(d, likesByDay.getOrDefault(d, 0L)));
        }

        List<Object[]> rawDailyFollows = artistFollowRepository.findDailyFollowsByArtist(artistId, since);
        Map<String, Long> followsByDay = new LinkedHashMap<>();
        for (Object[] row : rawDailyFollows) followsByDay.put(row[0].toString(), ((Number) row[1]).longValue());
        long sumNewInPeriod = followsByDay.values().stream().mapToLong(Long::longValue).sum();
        long baseFollowers  = Math.max(0, totalFollowers - sumNewInPeriod);
        List<StatsResponse.DayFollower> dailyFollowers = new ArrayList<>();
        long running = baseFollowers;
        for (int i = days - 1; i >= 0; i--) {
            String d = now.minusDays(i).toLocalDate().format(fmt);
            running += followsByDay.getOrDefault(d, 0L);
            dailyFollowers.add(new StatsResponse.DayFollower(d, running));
        }

        List<Object[]> rawTop = playRepository.findTopTracksByArtist(artistId, since);
        List<StatsResponse.TrackStat> topTracks = new ArrayList<>();
        for (Object[] row : rawTop) {
            Long   trackId    = ((Number) row[0]).longValue();
            String title      = (String) row[1];
            String coverUrl   = (String) row[2];
            String audioUrl   = (String) row[3];
            long   trackPlays = ((Number) row[4]).longValue();
            long   trackLikes = likeRepository.countByTrackId(trackId);
            topTracks.add(new StatsResponse.TrackStat(trackId, title, coverUrl, audioUrl, trackPlays, trackLikes));
        }
        if (topTracks.isEmpty()) {
            trackRepository.findByArtistProfileId(artistId).stream().limit(5).forEach(t ->
                    topTracks.add(new StatsResponse.TrackStat(
                            t.getId(), t.getTitle(), t.getCoverUrl(), t.getAudioUrl(),
                            playRepository.countByTrackId(t.getId()),
                            likeRepository.countByTrackId(t.getId())
                    ))
            );
        }

        List<Object[]> rawSources = playRepository.findSourceBreakdownByArtist(artistId, since);
        long totalSourcePlays = rawSources.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<StatsResponse.BreakdownItem> sources = new ArrayList<>();
        for (Object[] row : rawSources) {
            String ctx   = row[0].toString();
            long   count = ((Number) row[1]).longValue();
            long   pct   = totalSourcePlays > 0 ? Math.round(count * 100.0 / totalSourcePlays) : 0;
            sources.add(new StatsResponse.BreakdownItem(contextLabel(ctx), pct, contextColor(ctx)));
        }

        List<Object[]> rawCountries = playRepository.findCountryBreakdownByArtist(artistId, since);
        List<StatsResponse.BreakdownItem> locations = new ArrayList<>();
        for (Object[] row : rawCountries) {
            String code  = row[0] != null ? row[0].toString() : "XX";
            long   count = ((Number) row[1]).longValue();
            locations.add(new StatsResponse.BreakdownItem(countryName(code), count, "ocean"));
        }

        Double avgCompletion = playRepository.findAvgCompletionRateByArtist(artistId, since);
        double completionRate = avgCompletion != null ? Math.min(100.0, Math.round(avgCompletion * 10.0) / 10.0) : 0.0;

        return new StatsResponse(kpis, dailyPlays, dailyLikes, dailyFollowers, topTracks, sources, locations, completionRate);
    }

    /** GET /api/stats/weekly-summary */
    @Transactional(readOnly = true)
    public String getWeeklySummary(ArtistProfile profile) {
        Long artistId = profile.getId();
        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime since7 = now.minusDays(7);
        LocalDateTime since14= now.minusDays(14);

        long plays        = playRepository.countByTrackArtistProfileIdAndPlayedAtAfter(artistId, since7);
        long prevPlays    = playRepository.countByTrackArtistProfileIdAndPlayedAtBetween(artistId, since14, since7);
        long likes        = likeRepository.countByTrackArtistProfileIdAndCreatedAtAfter(artistId, since7);
        long newFollowers = artistFollowRepository.countByArtistProfileIdAndCreatedAtAfter(artistId, since7);
        long comments     = commentRepository.countByArtistProfileAndCreatedAtAfter(artistId, since7);

        List<Object[]> rawTop = playRepository.findTopTracksByArtist(artistId, since7);
        String topTrack = rawTop.isEmpty() ? null : (String) rawTop.get(0)[1];
        long   topPlays = rawTop.isEmpty() ? 0    : ((Number) rawTop.get(0)[4]).longValue();

        double playDelta = prevPlays > 0 ? ((double)(plays - prevPlays) / prevPlays) * 100.0 : 0;
        String trend = playDelta > 5 ? "subiu" : playDelta < -5 ? "desceu" : "manteve-se estável";
        String artistName = profile.getName() != null ? profile.getName() : "Artista";

        String systemPrompt = "És um assistente de dados para artistas musicais da plataforma Batuku. "
                + "Escreve um resumo semanal em português europeu, com 2 a 3 frases diretas e motivadoras. "
                + "Usa os dados fornecidos, não inventes valores. Sem markdown, sem listas, sem títulos.";

        String userPrompt = String.format(
                "Artista: %s. Esta semana: %d reproduções (%s %.0f%% face à semana anterior), "
                + "%d likes, %d novos seguidores, %d comentários. "
                + "%s"
                + "Escreve o resumo semanal.",
                artistName, plays, trend, Math.abs(playDelta),
                likes, newFollowers, comments,
                topTrack != null ? String.format("A faixa mais ouvida foi \"%s\" com %d reproduções. ", topTrack, topPlays) : "");

        String summary = groqClient.generateInsight(systemPrompt, userPrompt);

        if (summary == null) {
            if (plays == 0) {
                summary = "Esta semana ainda não tens reproduções registadas. Partilha as tuas faixas para começar a crescer!";
            } else {
                summary = String.format(
                        "Esta semana tiveste %d reproduções e %d novo%s seguidor%s. "
                        + (topTrack != null ? "A tua faixa mais ouvida foi \"%s\". " : "")
                        + "Continua a publicar para manter o crescimento.",
                        plays, newFollowers, newFollowers == 1 ? "" : "s", newFollowers == 1 ? "" : "s",
                        topTrack != null ? topTrack : "");
            }
        }
        return summary;
    }

    /** GET /api/stats/me/forecast */
    @Transactional(readOnly = true)
    public ForecastResponse getForecast(Long artistId, int historyDays, int forecastDays) {
        return forecastService.forecastPlays(artistId, historyDays, forecastDays);
    }

    /** POST /api/tracks/{id}/play — devolve o playId ou empty se a play não deve ser registada. */
    @Transactional
    public Optional<Long> recordPlay(Long trackId, String country, String context, User user) {
        Track track = trackRepository.findById(trackId).orElse(null);
        if (track == null) return Optional.empty();
        Play play = new Play();
        play.setTrack(track);
        play.setSource(Play.PlaySource.WEB);
        if (country != null) play.setCountry(country);
        if (context  != null) play.setReferrerUrl(context);
        if (user != null) {
            var owner = track.getArtistProfile();
            if (owner != null && owner.getUser() != null && owner.getUser().getId().equals(user.getId())) {
                return Optional.empty();
            }
            play.setUser(user);
        }
        Play saved = playRepository.save(play);
        return Optional.of(saved.getId());
    }

    /** PATCH /api/plays/{id} */
    public void updatePlay(Long playId, Integer durationPlayed, Boolean isFullPlay) {
        playRepository.findById(playId).ifPresent(play -> {
            if (durationPlayed != null) play.setDurationPlayed(durationPlayed);
            boolean jaEraFull = play.isFullPlay();
            if (isFullPlay != null) play.setFullPlay(isFullPlay);
            playRepository.save(play);
            if (!jaEraFull && play.isFullPlay() && play.getUser() != null) {
                try {
                    gamificationService.adicionarPontos(
                            play.getUser(),
                            PointTransaction.ActionType.PLAY,
                            play.getTrack().getId()
                    );
                } catch (Exception ignored) {}
            }
        });
    }

    private double delta(long current, long prev) {
        if (prev == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round(((double)(current - prev) / prev) * 1000.0) / 10.0;
    }

    private static String contextLabel(String ctx) {
        return switch (ctx) {
            case "playlist"     -> "Playlists";
            case "artist"       -> "Perfil artista";
            case "library"      -> "Biblioteca";
            case "home"         -> "Início";
            case "discover"     -> "Descobrir";
            case "release"      -> "Lançamento";
            case "direct"       -> "Direto";
            case "following"    -> "A seguir";
            case "fan",
                 "user-profile" -> "Perfis";
            default             -> ctx;
        };
    }

    private static String contextColor(String ctx) {
        return switch (ctx) {
            case "playlist"     -> "coral";
            case "artist"       -> "mustard";
            case "library"      -> "green";
            case "home"         -> "purple";
            case "discover"     -> "ocean";
            case "following"    -> "purple";
            case "fan",
                 "user-profile" -> "mute";
            default             -> "default";
        };
    }

    private static String countryName(String code) {
        return switch (code.toUpperCase()) {
            case "PT" -> "Portugal";
            case "CV" -> "Cabo Verde";
            case "BR" -> "Brasil";
            case "AO" -> "Angola";
            case "MZ" -> "Moçambique";
            case "FR" -> "França";
            case "NL" -> "Países Baixos";
            case "US" -> "EUA";
            case "UK", "GB" -> "Reino Unido";
            case "DE" -> "Alemanha";
            case "LU" -> "Luxemburgo";
            case "CH" -> "Suíça";
            case "ES" -> "Espanha";
            case "IT" -> "Itália";
            default   -> code;
        };
    }
}
