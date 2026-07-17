package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.storage.FileCategory;
import org.example.batuku.storage.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class MediaController {

    private final FileStorageService storageService;
    private final UserRepository userRepository;

    public MediaController(FileStorageService storageService, UserRepository userRepository) {
        this.storageService = storageService;
        this.userRepository = userRepository;
    }

    @PutMapping(value = "/avatar", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Não autenticado"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo de ficheiro inválido. Só são aceites imagens."));
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "O ficheiro de avatar não pode exceder 5 MB."));
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        String key = storageService.store(file, FileCategory.AVATAR);
        String url = storageService.resolveUrl(key, FileCategory.AVATAR);
        user.setAvatarUrl(url);
        userRepository.save(user);

        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping(value = "/audio", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo de ficheiro inválido. Só são aceites ficheiros de áudio."));
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "O ficheiro de áudio não pode exceder 20 MB."));
        }

        String key = storageService.store(file, FileCategory.AUDIO);
        String url = storageService.resolveUrl(key, FileCategory.AUDIO);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("key", key, "url", url));
    }
}
