package org.example.batuku.dto;

import java.math.BigDecimal;

public class OfferRequest {
    private Long beatId;
    private BigDecimal amount;
    private String message;

    public Long getBeatId() { return beatId; }
    public void setBeatId(Long beatId) { this.beatId = beatId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
