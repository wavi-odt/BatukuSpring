package org.example.batuku.controllers;

import org.example.batuku.dto.AdminMetricsResponse;
import org.example.batuku.services.AdminMetricsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/metrics")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class AdminMetricsController {

    private final AdminMetricsService adminMetricsService;

    public AdminMetricsController(AdminMetricsService adminMetricsService) {
        this.adminMetricsService = adminMetricsService;
    }

    @GetMapping
    public AdminMetricsResponse getMetrics() {
        return adminMetricsService.getMetrics();
    }
}
