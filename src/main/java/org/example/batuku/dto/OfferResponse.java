package org.example.batuku.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfferResponse {
    private Long id;
    private Long beatId;
    private String beatTitle;
    private String beatCoverUrl;
    private Integer beatHue;
    private Long fanId;
    private String fanName;
    private String fanHandle;
    private String fanAvatarUrl;
    private Long producerId;
    private String producerName;
    private BigDecimal amount;
    private String message;
    private String status;
    private String producerReply;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBeatId() { return beatId; }
    public void setBeatId(Long beatId) { this.beatId = beatId; }

    public String getBeatTitle() { return beatTitle; }
    public void setBeatTitle(String beatTitle) { this.beatTitle = beatTitle; }

    public String getBeatCoverUrl() { return beatCoverUrl; }
    public void setBeatCoverUrl(String beatCoverUrl) { this.beatCoverUrl = beatCoverUrl; }

    public Integer getBeatHue() { return beatHue; }
    public void setBeatHue(Integer beatHue) { this.beatHue = beatHue; }

    public Long getFanId() { return fanId; }
    public void setFanId(Long fanId) { this.fanId = fanId; }

    public String getFanName() { return fanName; }
    public void setFanName(String fanName) { this.fanName = fanName; }

    public String getFanHandle() { return fanHandle; }
    public void setFanHandle(String fanHandle) { this.fanHandle = fanHandle; }

    public String getFanAvatarUrl() { return fanAvatarUrl; }
    public void setFanAvatarUrl(String fanAvatarUrl) { this.fanAvatarUrl = fanAvatarUrl; }

    public Long getProducerId() { return producerId; }
    public void setProducerId(Long producerId) { this.producerId = producerId; }

    public String getProducerName() { return producerName; }
    public void setProducerName(String producerName) { this.producerName = producerName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProducerReply() { return producerReply; }
    public void setProducerReply(String producerReply) { this.producerReply = producerReply; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
}
