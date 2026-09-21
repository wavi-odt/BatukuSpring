package org.example.batuku.services;

import org.example.batuku.domain.PersonalUpload;
import org.example.batuku.domain.User;
import org.example.batuku.repository.PersonalUploadRepository;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PersonalUploadService {

    private static final long MAX_AUDIO_BYTES = 60L * 1024 * 1024;

    private final PersonalUploadRepository repo;
    private final FileStorageService fileStorage;

    public PersonalUploadService(PersonalUploadRepository repo, FileStorageService fileStorage) {
        this.repo = repo;
        this.fileStorage = fileStorage;
    }

    public PersonalUpload upload(User user, String title, MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file is required");
        }
        String contentType = audio.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be an audio file");
        }
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file must not exceed 60 MB");
        }

        String key = fileStorage.store(audio, FileCategory.PERSONAL);
        String audioUrl = fileStorage.resolveUrl(key, FileCategory.PERSONAL);
        String shareToken = UUID.randomUUID().toString();

        return repo.save(new PersonalUpload(user, title, audioUrl, shareToken));
    }

    public List<PersonalUpload> listMine(User user) {
        return repo.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public PersonalUpload findByShareToken(String token) {
        return repo.findByShareToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shared upload not found"));
    }

    public PersonalUpload regenerateShareToken(User user, Long id) {
        PersonalUpload upload = repo.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload not found"));
        upload.setShareToken(UUID.randomUUID().toString());
        return repo.save(upload);
    }

    public void delete(User user, Long id) {
        PersonalUpload upload = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload not found"));
        if (!upload.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this upload");
        }
        repo.delete(upload);
    }
}
