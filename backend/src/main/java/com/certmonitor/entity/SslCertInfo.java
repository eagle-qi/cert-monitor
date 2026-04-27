package com.certmonitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ssl_cert_info")
public class SslCertInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "asset_id", nullable = false)
    private Long assetId;
    
    @Column(length = 500)
    private String issuer;
    
    @Column(length = 500)
    private String subject;
    
    @Column(name = "valid_start")
    private LocalDateTime validStart;
    
    @Column(name = "valid_end")
    private LocalDateTime validEnd;
    
    @Column(name = "remain_days")
    private Integer remainDays;
    
    @Column(name = "risk_level")
    private Integer riskLevel;
    
    @Column(name = "cert_fingerprint", length = 100)
    private String certFingerprint;
    
    @Column(name = "san_names", columnDefinition = "TEXT")
    private String sanNames;
    
    @Column(name = "serial_number", length = 100)
    private String serialNumber;
    
    @Column(length = 50)
    private String algorithm;
    
    @Column(name = "scan_time")
    private LocalDateTime scanTime;
    
    @PrePersist
    protected void onCreate() {
        scanTime = LocalDateTime.now();
    }
    
    /**
     * 计算风险等级
     * 0: 正常 (>30天)
     * 1: 预警 (15-30天)
     * 2: 高危 (1-15天)
     * 3: 过期 (<=0天)
     */
    public void calculateRiskLevel() {
        if (remainDays == null || remainDays <= 0) {
            riskLevel = 3;
        } else if (remainDays <= 15) {
            riskLevel = 2;
        } else if (remainDays <= 30) {
            riskLevel = 1;
        } else {
            riskLevel = 0;
        }
    }
}
