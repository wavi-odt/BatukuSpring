package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_tokens")
public class LoginToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private String redirectPath;

    public LoginToken() {}

    public LoginToken(String token, User user, LocalDateTime expiresAt, String redirectPath) {
        this.token        = token;
        this.user         = user;
        this.expiresAt    = expiresAt;
        this.redirectPath = redirectPath;
    }

    public Long getId()                    { return id; }
    public String getToken()               { return token; }
    public User getUser()                  { return user; }
    public LocalDateTime getExpiresAt()    { return expiresAt; }
    public boolean isUsed()                { return used; }
    public String getRedirectPath()        { return redirectPath; }
    public void setUsed(boolean used)      { this.used = used; }
}
