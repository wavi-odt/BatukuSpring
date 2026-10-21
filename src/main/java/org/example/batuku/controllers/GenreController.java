package org.example.batuku.controllers;

import org.example.batuku.dto.GenreResponse;
import org.example.batuku.dto.ReleaseResponse;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.services.GenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<GenreResponse>>> getAll() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }

    @GetMapping("/{id}/tracks")
    public ResponseEntity<List<TrackResponse>> getTracks(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(genreService.getTracksByGenre(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/releases")
    public ResponseEntity<List<ReleaseResponse>> getReleases(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(genreService.getReleasesByGenre(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
