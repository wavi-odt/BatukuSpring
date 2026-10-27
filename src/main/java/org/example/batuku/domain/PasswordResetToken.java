package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId()                         { return id; }
    public String getToken()                    { return token; }
    public void setToken(String token)          { this.token = token; }
    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }
    public LocalDateTime getExpiresAt()         { return expiresAt; }
    public void setExpiresAt(LocalDateTime t)   { this.expiresAt = t; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
}
