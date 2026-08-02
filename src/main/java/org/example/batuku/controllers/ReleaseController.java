package org.example.batuku.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ReleaseResponse;
import org.example.batuku.services.AlbumService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/releases")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class ReleaseController {

    private static final Pattern TRACK_TITLE_PATTERN = Pattern.compile("^tracks\\[(\\d+)]\\.title$");
    private static final Pattern TRACK_AUDIO_PATTERN = Pattern.compile("^tracks\\[(\\d+)]\\.audio$");

    private final AlbumService albumService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public ReleaseController(AlbumService albumService, JwtUserDetailsService jwtUserDetailsService) {
        this.albumService = albumService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<ReleaseResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String title,
            @RequestParam String genre,
            @RequestParam String releaseType,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            HttpServletRequest request) {

        Map<Integer, String> trackTitles = new TreeMap<>();
        Map<Integer, MultipartFile> trackAudios = new TreeMap<>();

        if (request instanceof MultipartHttpServletRequest multipart) {
            for (Map.Entry<String, String[]> entry : multipart.getParameterMap().entrySet()) {
                Matcher m = TRACK_TITLE_PATTERN.matcher(entry.getKey());
                if (m.matches() && entry.getValue().length > 0) {
                    trackTitles.put(Integer.parseInt(m.group(1)), entry.getValue()[0]);
                }
            }
            for (Map.Entry<String, MultipartFile> entry : multipart.getFileMap().entrySet()) {
                Matcher m = TRACK_AUDIO_PATTERN.matcher(entry.getKey());
                if (m.matches()) {
                    trackAudios.put(Integer.parseInt(m.group(1)), entry.getValue());
                }
            }
        }

        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        ReleaseResponse response = albumService.createRelease(user, title, genre, releaseType, cover, trackTitles, trackAudios);

        return ResponseEntity.created(URI.create("/api/releases/" + response.getId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ReleaseResponse getRelease(@PathVariable Long id) {
        return albumService.findById(id);
    }

    @GetMapping("/artist/{artistProfileId}")
    public List<ReleaseResponse> listByArtist(@PathVariable Long artistProfileId) {
        return albumService.listByArtist(artistProfileId);
    }
}
