package com.certmonitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "scan_result")
public class ScanResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "asset_id", nullable = false)
    private Long assetId;
    
    @Column(name = "is_accessible")
    private Integer isAccessible = 0;
    
    @Column(name = "status_code")
    private Integer statusCode;
    
    @Column(name = "response_time")
    private Integer responseTime;
    
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    @Column(name = "scan_time")
    private LocalDateTime scanTime;
    
    @PrePersist
    protected void onCreate() {
        scanTime = LocalDateTime.now();
    }
}
