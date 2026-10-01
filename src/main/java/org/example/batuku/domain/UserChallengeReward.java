package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_challenge_rewards",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "challenge_id", "set_offset"}))
public class UserChallengeReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "challenge_id", nullable = false, length = 30)
    private String challengeId;

    @Column(name = "set_offset", nullable = false)
    private int setOffset;

    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt = LocalDateTime.now();

    public UserChallengeReward() {}

    public UserChallengeReward(User user, String challengeId, int setOffset) {
        this.user        = user;
        this.challengeId = challengeId;
        this.setOffset   = setOffset;
    }

    public Long getId()            { return id; }
    public User getUser()          { return user; }
    public String getChallengeId() { return challengeId; }
    public int getSetOffset()      { return setOffset; }
    public LocalDateTime getAwardedAt() { return awardedAt; }
}
