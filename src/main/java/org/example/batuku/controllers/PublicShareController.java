package org.example.batuku.controllers;

import org.example.batuku.domain.PersonalUpload;
import org.example.batuku.services.PersonalUploadService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shared")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class PublicShareController {

    private final PersonalUploadService service;

    public PublicShareController(PersonalUploadService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public SharedUploadResponse get(@PathVariable String token) {
        PersonalUpload upload = service.findByShareToken(token);
        return new SharedUploadResponse(upload.getTitle(), upload.getAudioUrl(), upload.getUser().getName());
    }

    record SharedUploadResponse(String title, String audioUrl, String sharedBy) {}
}
