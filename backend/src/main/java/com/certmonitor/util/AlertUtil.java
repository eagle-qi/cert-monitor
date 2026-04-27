package com.certmonitor.util;

import com.certmonitor.entity.AlertConfig;
import com.certmonitor.entity.AlertRecord;
import com.certmonitor.entity.DomainAsset;
import com.certmonitor.entity.SslCertInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    private static final Map<String, Long> alertCooldown = new HashMap<>();
    private static final long COOLDOWN_MINUTES = 60;
    
    /**
     * 检查是否在冷却期内
     */
    public boolean isInCooldown(Long assetId, String alertType) {
        String key = assetId + "_" + alertType;
        Long lastAlert = alertCooldown.get(key);
        if (lastAlert == null) return false;
        
        long minutesSinceLastAlert = (System.currentTimeMillis() - lastAlert) / 60000;
        return minutesSinceLastAlert < COOLDOWN_MINUTES;
    }
    
    /**
     * 记录告警发送时间
     */
    public void recordAlertSent(Long assetId, String alertType) {
        String key = assetId + "_" + alertType;
        alertCooldown.put(key, System.currentTimeMillis());
    }
    
    /**
     * 发送邮件告警
     */
    public void sendEmailAlert(AlertConfig config, AlertRecord record, DomainAsset asset, SslCertInfo cert) {
        try {
            JsonNode json = objectMapper.readTree(config.getConfig());
            String toEmail = json.get("toEmail").asText();
            String title = buildCertAlertTitle(cert, asset);
            String content = buildCertAlertContent(record, asset, cert);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(title);
            message.setText(content);
            
            mailSender.send(message);
            log.info("邮件告警发送成功: {}", toEmail);
            
        } catch (Exception e) {
            log.error("邮件告警发送失败: {}", e.getMessage());
        }
    }
    
    /**
     * 发送企业微信 Webhook 告警
     */
    public void sendWechatAlert(AlertConfig config, AlertRecord record, DomainAsset asset, SslCertInfo cert) {
        try {
            JsonNode json = objectMapper.readTree(config.getConfig());
            String webhookUrl = json.get("webhookUrl").asText();
            
            String color = getRiskLevelColor(cert.getRiskLevel());
            String levelText = getRiskLevelText(cert.getRiskLevel());
            
            String jsonBody = String.format("""
                {
                    "msgtype": "markdown",
                    "markdown": {
                        "content": "## 🔔 证书告警通知\\n\\n" +
                                   "**告警等级**: <font color=\\"%s\\">%s</font>\\n\\n" +
                                   "**资产名称**: %s\\n" +
                                   "**URL**: %s\\n\\n" +
                                   "**证书主体**: %s\\n" +
                                   "**颁发机构**: %s\\n\\n" +
                                   "**过期时间**: %s\\n" +
                                   "**剩余天数**: <font color=\\"%s\\">**%d 天**</font>\\n\\n" +
                                   "**建议**: %s"
                    }
                }
                """,
                    color, levelText,
                    asset.getBusinessGroup() != null ? asset.getBusinessGroup() : asset.getDomain(),
                    asset.getUrl(),
                    cert.getSubject(),
                    cert.getIssuer(),
                    cert.getValidEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    color, cert.getRemainDays(),
                    getSuggestion(cert.getRiskLevel())
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("企业微信告警发送成功");
            
        } catch (Exception e) {
            log.error("企业微信告警发送失败: {}", e.getMessage());
        }
    }
    
    /**
     * 发送钉钉 Webhook 告警
     */
    public void sendDingtalkAlert(AlertConfig config, AlertRecord record, DomainAsset asset, SslCertInfo cert) {
        try {
            JsonNode json = objectMapper.readTree(config.getConfig());
            String webhookUrl = json.get("webhookUrl").asText();
            String secret = json.has("secret") ? json.get("secret").asText() : null;
            
            String sign = "";
            if (secret != null) {
                sign = generateDingtalkSign(secret);
                webhookUrl = webhookUrl + "&sign=" + sign;
            }
            
            String color = getRiskLevelColor(cert.getRiskLevel());
            String levelText = getRiskLevelText(cert.getRiskLevel());
            
            String jsonBody = String.format("""
                {
                    "msgtype": "markdown",
                    "markdown": {
                        "title": "证书告警通知",
                        "content": "## 🔔 证书告警通知\\n\\n" +
                                   "**告警等级**: %s\\n\\n" +
                                   "**资产名称**: %s\\n" +
                                   "**URL**: %s\\n\\n" +
                                   "**证书主体**: %s\\n" +
                                   "**颁发机构**: %s\\n\\n" +
                                   "**过期时间**: %s\\n" +
                                   "**剩余天数**: **%d 天**\\n\\n" +
                                   "**建议**: %s"
                    }
                }
                """,
                    levelText,
                    asset.getBusinessGroup() != null ? asset.getBusinessGroup() : asset.getDomain(),
                    asset.getUrl(),
                    cert.getSubject(),
                    cert.getIssuer(),
                    cert.getValidEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    cert.getRemainDays(),
                    getSuggestion(cert.getRiskLevel())
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("钉钉告警发送成功");
            
        } catch (Exception e) {
            log.error("钉钉告警发送失败: {}", e.getMessage());
        }
    }
    
    /**
     * 生成钉钉签名
     */
    private String generateDingtalkSign(String secret) throws Exception {
        // 简化实现，实际应该使用 HMAC-SHA256
        return "";
    }
    
    /**
     * 构建证书告警标题
     */
    private String buildCertAlertTitle(SslCertInfo cert, DomainAsset asset) {
        String level = getRiskLevelText(cert.getRiskLevel());
        String domain = asset.getDomain();
        return String.format("[%s] 证书告警 - %s", level, domain);
    }
    
    /**
     * 构建证书告警内容
     */
    private String buildCertAlertContent(AlertRecord record, DomainAsset asset, SslCertInfo cert) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 证书告警通知 ===\\n\\n");
        sb.append("告警等级: ").append(getRiskLevelText(cert.getRiskLevel())).append("\\n");
        sb.append("URL: ").append(asset.getUrl()).append("\\n");
        if (asset.getBusinessGroup() != null) {
            sb.append("业务分组: ").append(asset.getBusinessGroup()).append("\\n");
        }
        if (asset.getOwner() != null) {
            sb.append("负责人: ").append(asset.getOwner()).append("\\n");
        }
        sb.append("\\n--- 证书信息 ---\\n");
        sb.append("证书主体: ").append(cert.getSubject()).append("\\n");
        sb.append("颁发机构: ").append(cert.getIssuer()).append("\\n");
        sb.append("生效时间: ").append(cert.getValidStart()).append("\\n");
        sb.append("过期时间: ").append(cert.getValidEnd()).append("\\n");
        sb.append("剩余天数: ").append(cert.getRemainDays()).append(" 天\\n");
        sb.append("\\n建议: ").append(getSuggestion(cert.getRiskLevel())).append("\\n");
        sb.append("\\n发送时间: ").append(record.getSendTime());
        return sb.toString();
    }
    
    private String getRiskLevelColor(Integer level) {
        if (level == null) return "#67C23A";
        switch (level) {
            case 1: return "#E6A23C";
            case 2: return "#F56C6C";
            case 3: return "#FF0000";
            default: return "#67C23A";
        }
    }
    
    private String getRiskLevelText(Integer level) {
        if (level == null) return "正常";
        switch (level) {
            case 1: return "⚠️ 预警";
            case 2: return "🔴 高危";
            case 3: return "❌ 已过期";
            default: return "✅ 正常";
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
