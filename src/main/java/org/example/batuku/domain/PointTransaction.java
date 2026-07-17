package org.example.batuku.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_transactions")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ActionType actionType;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ActionType { PLAY, LIKE, COMMENT, SHARE, MISSION_COMPLETE }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
