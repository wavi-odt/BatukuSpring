package org.example.batuku.controllers;

import org.example.batuku.dto.UserResponse;
import org.example.batuku.services.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class AdminUserController {

    private final UserProfileService userProfileService;

    public AdminUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public List<UserResponse> listUsers(@RequestParam(required = false) String q) {
        return userProfileService.listUsers(q);
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<UserResponse> toggleVerify(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userProfileService.toggleVerify(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
