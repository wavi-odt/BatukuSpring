package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.RegisterRequest;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de autenticação do Batuku.
 *
 * Endpoints:
 *   POST /api/auth/register  → criar conta nova
 *   GET  /api/auth/me        → dados do utilizador autenticado
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/auth/register
     *
     * Cria uma conta nova. Não requer autenticação (está em permitAll).
     *
     * Body exemplo:
     * {
     *   "email": "joao@exemplo.com",
     *   "username": "joao123",
     *   "password": "minha_pass",
     *   "name": "João Silva",
     *   "country": "PT"   (opcional)
     * }
     *
     * Resposta 201 Created:
     * { "id": 1, "email": "...", "name": "...", "userRole": "FAN", ... }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User created = authService.register(request);
            // Devolvemos UserResponse — nunca a entidade diretamente (teria a password!)
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
        } catch (RuntimeException ex) {
            // Email duplicado ou outro erro de negócio
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/auth/me
     *
     * Devolve os dados do utilizador que fez o pedido.
     * Requer Bearer token no header Authorization.
     *
     * O Spring Security já validou o JWT no JwtRequestFilter.
     * Aqui apenas lemos o email do SecurityContext e buscamos o utilizador.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Não autenticado"));
        }

        String email = auth.getName(); // o nome é o email (definido no JwtUserDetailsService)

        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
