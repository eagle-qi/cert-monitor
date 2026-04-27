package com.certmonitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alert_config")
public class AlertConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 100)
    private String name;
    
    @Column(name = "alert_type", length = 50)
    private String alertType;
    
    @Column(name = "risk_levels", length = 50)
    private String riskLevels;
    
    @Column(nullable = false)
    private Integer enabled = 1;
    
    @Column(name = "email_enabled")
    private Boolean emailEnabled = false;
    
    @Column(name = "email_to", length = 200)
    private String emailTo;
    
    @Column(name = "webhook_enabled")
    private Boolean webhookEnabled = false;
    
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;
    
    @Column(name = "dingtalk_enabled")
    private Boolean dingtalkEnabled = false;
    
    @Column(name = "dingtalk_webhook_url", length = 500)
    private String dingtalkWebhookUrl;
    
    @Column(name = "wechat_enabled")
    private Boolean wechatEnabled = false;
    
    @Column(name = "wechat_webhook_url", length = 500)
    private String wechatWebhookUrl;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
    
    public boolean appliesToRiskLevel(Integer level) {
        if (riskLevels == null || riskLevels.isEmpty()) return true;
        if (level == null) return true;
        for (String s : riskLevels.split(",")) {
            if (s.trim().equals(String.valueOf(level))) return true;
        }
        return false;
    }
}
