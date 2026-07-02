package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidade central da aplicação.
 *
 * Todo o utilizador (fã, artista ou admin) é um User.
 * O campo "role" (fan/artist/admin) existe como enum simples
 * para lógica de negócio, enquanto a tabela "roles" (ManyToMany)
 * serve o Spring Security.
 *
 * Separámos as duas coisas para não misturar responsabilidades:
 *   - roles (Spring Security) → controla o que podes chamar na API
 *   - userRole (negócio)      → determina funcionalidades disponíveis
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Email é o identificador de login — tem de ser único
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Handle único do utilizador (ex: @joao123) — também pode ser usado no login
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // Password armazenada em hash BCrypt (nunca em texto simples)
    @Column(nullable = false, length = 255)
    private String password;

    // Nome de apresentação do utilizador
    @Column(nullable = false, length = 120)
    private String name;

    // Papel no sistema: FAN, ARTIST ou ADMIN
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole userRole = UserRole.FAN;

    // URL do avatar (opcional — pode ser null)
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // País do utilizador (opcional)
    @Column(length = 100)
    private String country;

    // Conta ativa? Podemos desativar sem apagar
    @Column(nullable = false)
    private boolean enabled = true;

    // Data de criação — preenchida automaticamente
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Roles para o Spring Security.
     * FetchType.EAGER é necessário porque o Spring Security
     * precisa das roles no momento da autenticação.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // ── Enum de papel no negócio ────────────────────────────────────

    public enum UserRole {
        FAN,     // utilizador comum (pode interagir, não publica como artista)
        ARTIST,  // artista verificado (pode publicar, aceder a analytics)
        ADMIN    // administrador da plataforma
    }

    // ── Getters / Setters ───────────────────────────────────────────

    public Long getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UserRole getUserRole() { return userRole; }
    public void setUserRole(UserRole userRole) { this.userRole = userRole; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
