package com.project.korex.ForeignTransfer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_fee")
public class TransferFeeAdmin {

    @Id
    private String currencyCode; // USD, EUR 등

    private double rate;         // 수수료 비율 (예: 0.01 = 1%)
    private int minFee;          // 최소 수수료
    private LocalDateTime updatedAt;

    // getter & setter
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    public int getMinFee() { return minFee; }
    public void setMinFee(int minFee) { this.minFee = minFee; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
