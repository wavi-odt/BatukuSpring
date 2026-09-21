package org.example.batuku.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PurchaseResponse {
    private Long id;
    private Long beatId;
    private String title;
    private String producer;
    private String genre;
    private String licenseType;
    private BigDecimal price;
    private LocalDateTime purchasedAt;
    private String audioUrl;
    private String coverUrl;
    private Integer hue;

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }
    public Long getBeatId()                    { return beatId; }
    public void setBeatId(Long v)              { this.beatId = v; }
    public String getTitle()                   { return title; }
    public void setTitle(String v)             { this.title = v; }
    public String getProducer()                { return producer; }
    public void setProducer(String v)          { this.producer = v; }
    public String getGenre()                   { return genre; }
    public void setGenre(String v)             { this.genre = v; }
    public String getLicenseType()             { return licenseType; }
    public void setLicenseType(String v)       { this.licenseType = v; }
    public BigDecimal getPrice()               { return price; }
    public void setPrice(BigDecimal v)         { this.price = v; }
    public LocalDateTime getPurchasedAt()      { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime v){ this.purchasedAt = v; }
    public String getAudioUrl()                { return audioUrl; }
    public void setAudioUrl(String v)          { this.audioUrl = v; }
    public String getCoverUrl()                { return coverUrl; }
    public void setCoverUrl(String v)          { this.coverUrl = v; }
    public Integer getHue()                    { return hue; }
    public void setHue(Integer v)              { this.hue = v; }
}
