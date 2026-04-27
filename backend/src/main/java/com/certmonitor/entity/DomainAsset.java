package com.certmonitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "domain_asset")
public class DomainAsset {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String url;
    
    @Column(length = 10)
    private String protocol = "https";
    
    @Column(length = 255)
    private String domain;
    
    @Column(name = "business_group", length = 100)
    private String businessGroup;
    
    @Column(length = 100)
    private String owner;
    
    @Column(length = 500)
    private String tags;
    
    @Column(nullable = false)
    private Integer status = 1;
    
    @Column(name = "is_whitelist")
    private Integer isWhitelist = 1;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (domain == null && url != null) {
            domain = extractDomain(url);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
    
    private String extractDomain(String url) {
        if (url == null) return null;
        try {
            String temp = url.replaceAll("^https?://", "").split("/")[0];
            return temp.split(":")[0];
        } catch (Exception e) {
            return url;
        }
    }
}
