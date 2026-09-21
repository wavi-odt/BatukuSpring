package org.example.batuku.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "beats")
public class Beat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producer_id", nullable = false)
    private User producer;

    @Column(nullable = false)
    private String title;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(length = 80)
    private String genre;

    @Column
    private Integer bpm;

    @Column(name = "musical_key", length = 10)
    private String musicalKey;

    @Column
    private Integer hue;

    @Column(name = "lease_price", precision = 10, scale = 2)
    private BigDecimal leasePrice;

    @Column(name = "premium_price", precision = 10, scale = 2)
    private BigDecimal premiumPrice;

    @Column(name = "exclusive_price", precision = 10, scale = 2)
    private BigDecimal exclusivePrice;

    @Column(nullable = false)
    private Integer plays = 0;

    @Column(nullable = false)
    private Integer sales = 0;

    @Column(name = "is_new", nullable = false)
    private boolean isNew = false;

    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    @Column(name = "exclusive_negotiable", nullable = false)
    private boolean exclusiveNegotiable = false;

    @Column(name = "sold_exclusively", nullable = false)
    private boolean soldExclusively = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }

    public User getProducer() { return producer; }
    public void setProducer(User producer) { this.producer = producer; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public Integer getBpm() { return bpm; }
    public void setBpm(Integer bpm) { this.bpm = bpm; }

    public String getMusicalKey() { return musicalKey; }
    public void setMusicalKey(String musicalKey) { this.musicalKey = musicalKey; }

    public Integer getHue() { return hue; }
    public void setHue(Integer hue) { this.hue = hue; }

    public BigDecimal getLeasePrice() { return leasePrice; }
    public void setLeasePrice(BigDecimal leasePrice) { this.leasePrice = leasePrice; }

    public BigDecimal getPremiumPrice() { return premiumPrice; }
    public void setPremiumPrice(BigDecimal premiumPrice) { this.premiumPrice = premiumPrice; }

    public BigDecimal getExclusivePrice() { return exclusivePrice; }
    public void setExclusivePrice(BigDecimal exclusivePrice) { this.exclusivePrice = exclusivePrice; }

    public Integer getPlays() { return plays; }
    public void setPlays(Integer plays) { this.plays = plays; }

    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }

    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean isFeatured) { this.isFeatured = isFeatured; }

    public boolean isExclusiveNegotiable() { return exclusiveNegotiable; }
    public void setExclusiveNegotiable(boolean exclusiveNegotiable) { this.exclusiveNegotiable = exclusiveNegotiable; }

    public boolean isSoldExclusively() { return soldExclusively; }
    public void setSoldExclusively(boolean soldExclusively) { this.soldExclusively = soldExclusively; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
