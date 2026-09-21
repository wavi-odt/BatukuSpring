package org.example.batuku.controllers;

import org.example.batuku.domain.PersonalProject;
import org.example.batuku.domain.PersonalProjectTrack;
import org.example.batuku.services.PersonalProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint público — sem autenticação.
 * Qualquer pessoa pode aceder a GET /api/shared-project/{token}.
 */
@RestController
@RequestMapping("/api/shared-project")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class SharedProjectController {

    private final PersonalProjectService service;

    public SharedProjectController(PersonalProjectService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public SharedProjectResponse get(@PathVariable String token) {
        PersonalProject project = service.findByShareTokenWithTracks(token);
        List<SharedTrackResponse> tracks = project.getTracks().stream()
                .map(t -> new SharedTrackResponse(t.getId(), t.getTitle(), t.getAudioUrl()))
                .toList();
        return new SharedProjectResponse(
                project.getName(),
                project.getCoverUrl(),
                project.getUser().getName(),
                tracks);
    }

    record SharedTrackResponse(Long id, String title, String audioUrl) {}

    record SharedProjectResponse(
            String name,
            String coverUrl,
            String sharedBy,
            List<SharedTrackResponse> tracks) {}
}
