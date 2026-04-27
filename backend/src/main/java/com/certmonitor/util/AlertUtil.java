package com.certmonitor.util;

import com.certmonitor.entity.AlertConfig;
import com.certmonitor.entity.AlertRecord;
import com.certmonitor.entity.DomainAsset;
import com.certmonitor.entity.SslCertInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AlertUtil {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    private static final Map<String, Long> alertCooldown = new HashMap<>();
    private static final long COOLDOWN_MINUTES = 60;
    
    public boolean isInCooldown(Long assetId, String alertType) {
        String key = assetId + "_" + alertType;
        Long lastAlert = alertCooldown.get(key);
        if (lastAlert == null) return false;
        long minutesSinceLastAlert = (System.currentTimeMillis() - lastAlert) / 60000;
        return minutesSinceLastAlert < COOLDOWN_MINUTES;
    }
    
    public void recordAlertSent(Long assetId, String alertType) {
        String key = assetId + "_" + alertType;
        alertCooldown.put(key, System.currentTimeMillis());
    }
    
    /**
     * 发送邮件告警 (直接从 AlertConfig 取字段)
     */
    public void sendEmail(AlertConfig config, String subject, String content) {
        try {
            if (config.getEmailTo() == null || config.getEmailTo().isEmpty()) return;
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(config.getEmailTo());
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("邮件告警发送成功: {}", config.getEmailTo());
        } catch (Exception e) {
            log.error("邮件告警发送失败: {}", e.getMessage());
        }
    }
    
    /**
     * 发送 Webhook 告警（通用，用于企业微信等）
     */
    public void sendWebhook(String webhookUrl, String subject, String content) {
        try {
            String jsonBody = String.format("""
                {
                    "msgtype": "markdown",
                    "markdown": {
                        "content": "%s\\n\\n%s"
                    }
                }
                """,
                    escapeJson(subject),
                    escapeJson(content));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Webhook 告警发送成功: {}", webhookUrl);
        } catch (Exception e) {
            log.error("Webhook 告警发送失败: {}", e.getMessage());
        }
    }
    
    public void sendWechat(String webhookUrl, String subject, String content) {
        sendWebhook(webhookUrl, subject, content);
    }
    
    public void sendDingtalk(String webhookUrl, String subject, String content) {
        sendWebhook(webhookUrl, subject, content);
    }
    
    // ====== 兼容旧接口的方法 ======
    
    public void sendEmailAlert(AlertConfig config, AlertRecord record, DomainAsset asset, SslCertInfo cert) {
        try {
            String title = buildCertAlertTitle(cert, asset);
            String contentStr = buildCertAlertContent(record, asset, cert);
            sendEmail(config, title, contentStr);
        } catch (Exception e) {
            log.error("邮件告警发送失败: {}", e.getMessage());
        }
    }
    
    public void sendWechatAlert(AlertConfig config, AlertRecord record, DomainAsset asset, SslCertInfo cert) {
        try {
            String webhookUrl = config.getWechatWebhookUrl();
            if (webhookUrl == null || webhookUrl.isEmpty()) return;
            String title = buildCertAlertTitle(cert, asset);
            String contentStr = buildCertAlertContent(record, asset, cert);
            sendWechat(webhookUrl, title, contentStr);
        } catch (Exception e) {
            log.error("企业微信告警发送失败: {}", e.getMessage());
        }
    }
    
    public void sendDingtalkAlert(AlertConfig config, AlertRecord record, DomainAsset asset, SslCertInfo cert) {
        try {
            String webhookUrl = config.getDingtalkWebhookUrl();
            if (webhookUrl == null || webhookUrl.isEmpty()) return;
            String title = buildCertAlertTitle(cert, asset);
            String contentStr = buildCertAlertContent(record, asset, cert);
            sendDingtalk(webhookUrl, title, contentStr);
        } catch (Exception e) {
            log.error("钉钉告警发送失败: {}", e.getMessage());
        }
    }
    
    private String buildCertAlertTitle(SslCertInfo cert, DomainAsset asset) {
        String level = getRiskLevelText(cert.getRiskLevel());
        String domain = asset.getDomain() != null ? asset.getDomain() : asset.getUrl();
        return String.format("[%s] 证书告警 - %s", level, domain);
    }
    
    private String buildCertAlertContent(AlertRecord record, DomainAsset asset, SslCertInfo cert) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 证书告警通知 ===\n\n");
        sb.append("告警等级: ").append(getRiskLevelText(cert.getRiskLevel())).append("\n");
        sb.append("URL: ").append(asset.getUrl()).append("\n");
        if (asset.getBusinessGroup() != null) {
            sb.append("业务分组: ").append(asset.getBusinessGroup()).append("\n");
        }
        if (asset.getOwner() != null) {
            sb.append("负责人: ").append(asset.getOwner()).append("\n");
        }
        sb.append("\n--- 证书信息 ---\n");
        if (cert.getSubject() != null) {
            sb.append("证书主体: ").append(cert.getSubject()).append("\n");
        }
        if (cert.getIssuer() != null) {
            sb.append("颁发机构: ").append(cert.getIssuer()).append("\n");
        }
        if (cert.getValidStart() != null) {
            sb.append("生效时间: ").append(cert.getValidStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        }
        if (cert.getValidEnd() != null) {
            sb.append("过期时间: ").append(cert.getValidEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        }
        sb.append("剩余天数: ").append(cert.getRemainDays() != null ? cert.getRemainDays() : "?").append(" 天\n");
        sb.append("\n建议: ").append(getSuggestion(cert.getRiskLevel())).append("\n");
        return sb.toString();
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
              .replace("\"", "\\\"")
              .replace("\n", "\\n")
              .replace("\r", "\\r");
    }
    
    private String getRiskLevelText(Integer level) {
        if (level == null) return "正常";
        switch (level) {
            case 1: return "预警";
            case 2: return "高危";
            case 3: return "已过期";
            default: return "正常";
        }
    }
    
    private String getSuggestion(Integer level) {
        if (level == null) return "证书状态正常，继续保持监控";
        switch (level) {
            case 1: return "请准备证书续期，预计15天内完成";
            case 2: return "紧急！请立即处理证书续期";
            case 3: return "证书已过期，请立即更换证书";
            default: return "证书状态正常，继续保持监控";
        }
    }
}
