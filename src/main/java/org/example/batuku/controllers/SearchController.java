package org.example.batuku.controllers;

import org.example.batuku.dto.SearchResponse;
import org.example.batuku.services.SearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(@RequestParam("q") String q) {
        return searchService.search(q);
    }
}
