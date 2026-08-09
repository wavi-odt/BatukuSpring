package org.example.batuku.controllers;

import org.example.batuku.config.StorageProperties;
import org.example.batuku.domain.ArtistFollow;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Follow;
import org.example.batuku.domain.Track;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.storage.AudioDurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/migrate")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMigrationController {

    private static final Logger log = LoggerFactory.getLogger(AdminMigrationController.class);

    private final TrackRepository trackRepository;
    private final FollowRepository followRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final Path baseDir;
    private final String publicBaseUrl;

    public AdminMigrationController(TrackRepository trackRepository,
                                    FollowRepository followRepository,
                                    ArtistFollowRepository artistFollowRepository,
                                    ArtistProfileRepository artistProfileRepository,
                                    StorageProperties storageProperties) {
        this.trackRepository = trackRepository;
        this.followRepository = followRepository;
        this.artistFollowRepository = artistFollowRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.baseDir = Path.of(storageProperties.getLocal().getBaseDir()).toAbsolutePath();
        this.publicBaseUrl = storageProperties.getLocal().getPublicBaseUrl();
    }

    /**
     * Migra follows antigos (User→User) para artist_follows (User→ArtistProfile)
     * quando o followee tem um ArtistProfile associado.
     * Seguro de correr múltiplas vezes — ignora entradas já migradas.
     */
    @PostMapping("/artist-follows")
    @Transactional
    public Map<String, Object> migrateArtistFollows() {
        List<Follow> all = followRepository.findAll();
        int migrated = 0;
        int skipped  = 0;
        int deleted  = 0;

        for (Follow follow : all) {
            Optional<ArtistProfile> profileOpt =
                    artistProfileRepository.findByUserId(follow.getFollowee().getId());

            if (profileOpt.isEmpty()) {
                skipped++;
                continue;
            }

            ArtistProfile profile = profileOpt.get();
            Long followerId      = follow.getFollower().getId();
            Long artistProfileId = profile.getId();

            if (!artistFollowRepository.existsByFollowerIdAndArtistProfileId(followerId, artistProfileId)) {
                ArtistFollow af = new ArtistFollow();
                af.setFollower(follow.getFollower());
                af.setArtistProfile(profile);
                artistFollowRepository.save(af);
                migrated++;
                log.info("Migrado: user {} → artist_profile {}", followerId, artistProfileId);
            } else {
                log.info("Já existia: user {} → artist_profile {} (apenas remove da tabela follows)", followerId, artistProfileId);
            }

            followRepository.delete(follow);
            deleted++;
        }

        log.info("Migração artist-follows: {} migrados, {} removidos de follows, {} ignorados (não são artistas)",
                migrated, deleted, skipped);
        return Map.of(
                "migrated", migrated,
                "deleted",  deleted,
                "skipped",  skipped
        );
    }

    /**
     * Preenche durationMs para todas as tracks UPLOAD que ainda não têm duração.
     * Só funciona com storage local. Chamar uma vez após deploy.
     */
    @PostMapping("/track-durations")
    public Map<String, Object> backfillTrackDurations() {
        List<Track> tracks = trackRepository.findBySourceAndDurationMsIsNull(Track.TrackSource.UPLOAD);

        int updated = 0;
        int skipped = 0;

        for (Track track : tracks) {
            Path file = resolveFilePath(track.getAudioUrl());
            if (file == null) {
                log.warn("Não foi possível resolver o caminho para track id={} url={}", track.getId(), track.getAudioUrl());
                skipped++;
                continue;
            }

            Integer durationMs = AudioDurationUtil.extractMs(file);
            if (durationMs == null) {
                log.warn("Não foi possível extrair duração da track id={} ficheiro={}", track.getId(), file.getFileName());
                skipped++;
                continue;
            }

            track.setDurationMs(durationMs);
            trackRepository.save(track);
            log.info("Track id={} → {}ms", track.getId(), durationMs);
            updated++;
        }

        log.info("Migração concluída: {} atualizadas, {} ignoradas", updated, skipped);
        return Map.of(
                "total",   tracks.size(),
                "updated", updated,
                "skipped", skipped
        );
    }

    /**
     * Deriva o caminho absoluto no disco a partir de audioUrl.
     * Funciona apenas para storage local — audioUrl = publicBaseUrl + "/" + relativeKey.
     */
    private Path resolveFilePath(String audioUrl) {
        if (audioUrl == null) return null;
        String prefix = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        if (!audioUrl.startsWith(prefix)) {
            // Tenta sem o host (caso a URL base seja diferente em dev vs prod)
            int pathStart = audioUrl.indexOf("/uploads/");
            if (pathStart < 0) return null;
            String relativePath = audioUrl.substring(pathStart + 1); // remove leading /
            return baseDir.getParent().resolve(relativePath);
        }
        String key = audioUrl.substring(prefix.length());
        return baseDir.resolve(key);
    }
}
