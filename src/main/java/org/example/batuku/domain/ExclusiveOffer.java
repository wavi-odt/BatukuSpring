package org.example.batuku.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exclusive_offers")
public class ExclusiveOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fan_id", nullable = false)
    private User fan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beat_id", nullable = false)
    private Beat beat;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String message;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "producer_reply", length = 500)
    private String producerReply;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    public Long getId() { return id; }

    public User getFan() { return fan; }
    public void setFan(User fan) { this.fan = fan; }

    public Beat getBeat() { return beat; }
    public void setBeat(Beat beat) { this.beat = beat; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProducerReply() { return producerReply; }
    public void setProducerReply(String producerReply) { this.producerReply = producerReply; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
}
