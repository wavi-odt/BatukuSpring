package org.example.batuku.services;

import com.mpatric.mp3agic.Mp3File;
import org.example.batuku.domain.PersonalProject;
import org.example.batuku.domain.PersonalProjectTrack;
import org.example.batuku.domain.User;
import org.example.batuku.repository.PersonalProjectRepository;
import org.example.batuku.repository.PersonalProjectTrackRepository;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;

@Service
public class PersonalProjectService {

    private static final long MAX_AUDIO_BYTES = 60L * 1024 * 1024;
    private static final long MAX_IMAGE_BYTES =  5L * 1024 * 1024;

    private final PersonalProjectRepository      projectRepo;
    private final PersonalProjectTrackRepository trackRepo;
    private final FileStorageService             fileStorage;

    public PersonalProjectService(PersonalProjectRepository projectRepo,
                                  PersonalProjectTrackRepository trackRepo,
                                  FileStorageService fileStorage) {
        this.projectRepo = projectRepo;
        this.trackRepo   = trackRepo;
        this.fileStorage = fileStorage;
    }

    /* ─── Projetos ──────────────────────────────────────────────── */

    @Transactional
    public PersonalProject createProject(User user, String name, MultipartFile cover) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name is required");
        }
        String coverUrl = null;
        if (cover != null && !cover.isEmpty()) {
            validateImage(cover);
            String key = fileStorage.store(cover, FileCategory.PROJECT_COVER);
            coverUrl = fileStorage.resolveUrl(key, FileCategory.PROJECT_COVER);
        }
        return projectRepo.save(new PersonalProject(user, name.strip(), coverUrl, UUID.randomUUID().toString()));
    }

    public List<PersonalProject> listMine(User user) {
        return projectRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public PersonalProject getProject(User user, Long id) {
        return projectRepo.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    @Transactional
    public void deleteProject(User user, Long id) {
        PersonalProject project = getProject(user, id);
        projectRepo.delete(project);
    }

    @Transactional
    public PersonalProject regenerateShareToken(User user, Long id) {
        PersonalProject project = getProject(user, id);
        project.setShareToken(UUID.randomUUID().toString());
        return projectRepo.save(project);
    }

    /* ─── Faixas do projeto ─────────────────────────────────────── */

    @Transactional
    public PersonalProjectTrack addTrack(User user, Long projectId, String title, MultipartFile audio) {
        PersonalProject project = getProject(user, projectId);
        validateAudio(audio);

        Integer duration = extractDurationSeconds(audio);

        String key      = fileStorage.store(audio, FileCategory.PERSONAL);
        String audioUrl = fileStorage.resolveUrl(key, FileCategory.PERSONAL);

        return trackRepo.save(new PersonalProjectTrack(project, title.strip(), audioUrl, duration));
    }

    public List<PersonalProjectTrack> listTracks(User user, Long projectId) {
        getProject(user, projectId); // garante ownership
        return trackRepo.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    @Transactional
    public void deleteTrack(User user, Long projectId, Long trackId) {
        getProject(user, projectId); // garante ownership
        PersonalProjectTrack track = trackRepo.findByIdAndProjectId(trackId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found in project"));
        trackRepo.delete(track);
    }

    @Transactional
    public PersonalProject updateProject(User user, Long id, String name, MultipartFile cover) {
        PersonalProject project = getProject(user, id);
        if (name != null && !name.isBlank()) {
            project.setName(name.strip());
        }
        if (cover != null && !cover.isEmpty()) {
            validateImage(cover);
            String key = fileStorage.store(cover, FileCategory.PROJECT_COVER);
            project.setCoverUrl(fileStorage.resolveUrl(key, FileCategory.PROJECT_COVER));
        }
        return projectRepo.save(project);
    }

    /* ─── Partilha pública ──────────────────────────────────────── */

    /** Uso interno — sem garantia de carregamento das faixas. */
    public PersonalProject findByShareToken(String token) {
        return projectRepo.findByShareToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    /** Uso público — carrega projeto + faixas num único JOIN. */
    public PersonalProject findByShareTokenWithTracks(String token) {
        return projectRepo.findByShareTokenWithTracks(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    /* ─── Duração do áudio ──────────────────────────────────────── */

    private Integer extractDurationSeconds(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) return null;
        String ct = audio.getContentType();
        try {
            if ("audio/mpeg".equals(ct) || "audio/mp3".equals(ct)) {
                return extractMp3Duration(audio);
            }
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(audio.getInputStream()))) {
                AudioFormat fmt = stream.getFormat();
                long frames = stream.getFrameLength();
                if (frames > 0 && fmt.getFrameRate() > 0) {
                    return (int) Math.round(frames / fmt.getFrameRate());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Integer extractMp3Duration(MultipartFile audio) {
        File tmp = null;
        try {
            tmp = File.createTempFile("batuku-dur-", ".mp3");
            try (var in = audio.getInputStream(); var out = new FileOutputStream(tmp)) {
                in.transferTo(out);
            }
            return (int) new Mp3File(tmp).getLengthInSeconds();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (tmp != null) tmp.delete();
        }
    }

    /* ─── Validações ────────────────────────────────────────────── */

    private void validateAudio(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file is required");
        }
        String ct = audio.getContentType();
        if (ct == null || !ct.startsWith("audio/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be an audio file");
        }
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file must not exceed 60 MB");
        }
    }

    private void validateImage(MultipartFile image) {
        String ct = image.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cover must be an image file");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cover image must not exceed 5 MB");
        }
    }
}
