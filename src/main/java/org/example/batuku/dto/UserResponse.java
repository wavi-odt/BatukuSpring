package org.example.batuku.dto;

import org.example.batuku.domain.User;
import java.time.LocalDateTime;

/**
 * DTO de resposta quando devolvemos dados de um utilizador.
 *
 * NUNCA devolvemos a password ao cliente — por isso existe este DTO
 * em vez de serializar a entidade User diretamente.
 *
 * Construtor estático "from(User)" facilita a conversão.
 */
public class UserResponse {

    private Long id;
    private String email;
    private String username;
    private String name;
    private String userRole;   // "FAN", "ARTIST" ou "ADMIN"
    private String avatarUrl;
    private String country;
    private LocalDateTime createdAt;

    // Construtor privado — obriga a usar o método estático
    private UserResponse() {}

    /**
     * Converte uma entidade User num UserResponse seguro para enviar ao cliente.
     */
    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id        = user.getId();
        r.email     = user.getEmail();
        r.username  = user.getUsername();
        r.name      = user.getName();
        r.userRole  = user.getUserRole().name();
        r.avatarUrl = user.getAvatarUrl();
        r.country   = user.getCountry();
        r.createdAt = user.getCreatedAt();
        return r;
    }

    // ── Getters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getUserRole() { return userRole; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getCountry() { return country; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
