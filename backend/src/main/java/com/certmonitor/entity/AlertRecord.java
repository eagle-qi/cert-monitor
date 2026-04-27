package com.certmonitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alert_record")
public class AlertRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "asset_id")
    private Long assetId;
    
    @Column(name = "alert_type", length = 20)
    private String alertType;
    
    @Column(name = "risk_level")
    private Integer riskLevel;
    
    @Column(length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "is_read")
    private Integer isRead = 0;
    
    @Column(name = "send_time")
    private LocalDateTime sendTime;
    
    @PrePersist
    protected void onCreate() {
        sendTime = LocalDateTime.now();
    }
}
