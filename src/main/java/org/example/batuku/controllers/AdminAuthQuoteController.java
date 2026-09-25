package org.example.batuku.controllers;

import org.example.batuku.services.AuthQuoteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth-quote")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuthQuoteController {

    private final AuthQuoteService authQuoteService;

    public AdminAuthQuoteController(AuthQuoteService authQuoteService) {
        this.authQuoteService = authQuoteService;
    }

    @GetMapping("/{page}")
    public AuthQuoteService.AuthQuoteAdminResponse get(@PathVariable String page) {
        return authQuoteService.getAdmin(page);
    }

    @PutMapping("/{page}")
    public void save(@PathVariable String page, @RequestBody SaveRequest request) {
        authQuoteService.save(page, request.quote(), request.artistProfileId());
    }

    @DeleteMapping("/{page}")
    public void clear(@PathVariable String page) {
        authQuoteService.clear(page);
    }

    record SaveRequest(String quote, Long artistProfileId) {}
}
