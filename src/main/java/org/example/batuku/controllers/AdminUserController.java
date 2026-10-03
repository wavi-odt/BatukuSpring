package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserResponse> listUsers(@RequestParam(required = false) String q) {
        List<User> users = (q != null && !q.isBlank())
                ? userRepository.searchUsers(q.trim(), PageRequest.of(0, 50))
                : userRepository.findTop50ByOrderByCreatedAtDesc();
        return users.stream().map(UserResponse::from).toList();
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<UserResponse> toggleVerify(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        user.setVerified(!user.isVerified());
        userRepository.save(user);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
