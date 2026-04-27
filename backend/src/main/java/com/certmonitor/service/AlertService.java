package com.certmonitor.service;

import com.certmonitor.entity.*;
import com.certmonitor.repository.AlertConfigRepository;
import com.certmonitor.repository.AlertRecordRepository;
import com.certmonitor.util.AlertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AlertService {
    
    @Autowired
    private AlertConfigRepository configRepository;
    
    @Autowired
    private AlertRecordRepository recordRepository;
    
    @Autowired
    private AlertUtil alertUtil;
    
    /**
     * 发送证书告警
     */
    @Transactional
    public void sendCertAlert(SslCertInfo cert, DomainAsset asset) {
        // 检查冷却期
        if (alertUtil.isInCooldown(asset.getId(), "cert")) {
            log.debug("证书告警在冷却期内: {}", asset.getUrl());
            return;
        }
        
        List<AlertConfig> configs = configRepository.findByEnabled(1);
        
        AlertRecord record = new AlertRecord();
        record.setAssetId(asset.getId());
        record.setAlertType("cert");
        record.setRiskLevel(cert.getRiskLevel());
        record.setTitle(buildAlertTitle(cert, asset));
        record.setMessage(buildAlertMessage(cert, asset));
        
        record = recordRepository.save(record);
        
        for (AlertConfig config : configs) {
            try {
                switch (config.getAlertType()) {
                    case "email":
                        alertUtil.sendEmailAlert(config, record, asset, cert);
                        break;
                    case "wechat":
                        alertUtil.sendWechatAlert(config, record, asset, cert);
                        break;
                    case "dingtalk":
                        alertUtil.sendDingtalkAlert(config, record, asset, cert);
                        break;
                }
            } catch (Exception e) {
                log.error("发送告警失败: type={}, error: {}", config.getAlertType(), e.getMessage());
            }
        }
        
        alertUtil.recordAlertSent(asset.getId(), "cert");
        log.info("证书告警发送完成: {}", asset.getUrl());
    }
    
    /**
     * 发送扫描失败告警
     */
    @Transactional
    public void sendScanAlert(ScanResult result, DomainAsset asset) {
        // 检查冷却期
        if (alertUtil.isInCooldown(asset.getId(), "scan")) {
            return;
        }
        
        List<AlertConfig> configs = configRepository.findByEnabled(1);
        
        AlertRecord record = new AlertRecord();
        record.setAssetId(asset.getId());
        record.setAlertType("scan");
        record.setRiskLevel(2);
        record.setTitle("URL 访问异常告警");
        record.setMessage(String.format("URL: %s\\n状态码: %s\\n响应耗时: %dms\\n错误信息: %s",
                asset.getUrl(),
                result.getStatusCode(),
                result.getResponseTime(),
                result.getErrorMessage()));
        
        recordRepository.save(record);
        
        // 只发送高危告警到企业微信/钉钉
        for (AlertConfig config : configs) {
            if ("wechat".equals(config.getAlertType()) || "dingtalk".equals(config.getAlertType())) {
                log.info("扫描告警已记录: {}", asset.getUrl());
            }
        }
        
        alertUtil.recordAlertSent(asset.getId(), "scan");
    }
    
    /**
     * 获取告警记录
     */
    public Page<AlertRecord> list(Pageable pageable, Integer isRead) {
        if (isRead != null) {
            return recordRepository.findByIsReadOrderBySendTimeDesc(isRead, pageable);
        }
        return recordRepository.findByOrderBySendTimeDesc(pageable);
    }
    
    /**
     * 标记已读
     */
    @Transactional
    public void markAsRead(Long id) {
        recordRepository.findById(id).ifPresent(record -> {
            record.setIsRead(1);
            recordRepository.save(record);
        });
    }
    
    /**
     * 标记所有已读
     */
    @Transactional
    public void markAllAsRead() {
        recordRepository.findByIsReadOrderBySendTimeDesc(0, Pageable.unpaged())
                .forEach(record -> {
                    record.setIsRead(1);
                    recordRepository.save(record);
                });
    }
    
    /**
     * 获取未读告警数量
     */
    public long getUnreadCount() {
        return recordRepository.countByIsRead(0);
    }
    
    /**
     * 保存告警配置
     */
    @Transactional
    public AlertConfig saveConfig(AlertConfig config) {
        return configRepository.save(config);
    }
    
    /**
     * 获取告警配置
     */
    public List<AlertConfig> getConfigs() {
        return configRepository.findAll();
    }
    
    /**
     * 获取特定类型的配置
     */
    public AlertConfig getConfigByType(String type) {
        return configRepository.findByAlertType(type).orElse(null);
    }
    
    /**
     * 删除告警配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        configRepository.deleteById(id);
    }
    
    private String buildAlertTitle(SslCertInfo cert, DomainAsset asset) {
        String level = "";
        if (cert.getRiskLevel() != null) {
            switch (cert.getRiskLevel()) {
                case 1: level = "预警"; break;
                case 2: level = "高危"; break;
                case 3: level = "已过期"; break;
                default: level = "正常"; break;
            }
        }
        return String.format("[%s] 证书告警 - %s", level, asset.getDomain());
    }
    
    private String buildAlertMessage(SslCertInfo cert, DomainAsset asset) {
        StringBuilder sb = new StringBuilder();
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
        sb.append("过期时间: ").append(cert.getValidEnd()).append("\\n");
        sb.append("剩余天数: ").append(cert.getRemainDays()).append(" 天");
        return sb.toString();
    }
    
    // 辅助方法：返回分页对象
    private static class Pageable {
        static org.springframework.data.domain.Pageable unpaged() {
            return org.springframework.data.domain.Pageable.unpaged();
        }
    }
}
