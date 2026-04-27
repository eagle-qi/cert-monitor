package com.certmonitor.service;

import com.certmonitor.entity.*;
import com.certmonitor.repository.*;
import com.certmonitor.util.AlertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AlertService {

    @Autowired
    private AlertConfigRepository alertConfigRepository;
    @Autowired
    private AlertRecordRepository alertRecordRepository;
    @Autowired
    private AlertUtil alertUtil;

    @Transactional
    public AlertConfig saveConfig(AlertConfig config) {
        if (config.getId() == null) {
            config.setCreateTime(LocalDateTime.now());
        }
        config.setUpdateTime(LocalDateTime.now());
        return alertConfigRepository.save(config);
    }

    public List<AlertConfig> getConfigs() {
        return alertConfigRepository.findAll();
    }

    public AlertConfig getConfigByType(String alertType) {
        return alertConfigRepository.findByAlertType(alertType).orElse(null);
    }

    public List<AlertConfig> getEnabledConfigs() {
        return alertConfigRepository.findByEnabled(1);
    }

    @Transactional
    public void deleteConfig(Long id) {
        alertConfigRepository.deleteById(id);
    }

    public Page<AlertRecord> list(Pageable pageable, Integer isRead) {
        if (isRead != null) {
            return alertRecordRepository.findByIsReadOrderBySendTimeDesc(isRead, pageable);
        }
        return alertRecordRepository.findByOrderBySendTimeDesc(pageable);
    }

    public long getUnreadCount() {
        return alertRecordRepository.countByIsRead(0);
    }

    @Transactional
    public void markAsRead(Long id) {
        AlertRecord record = alertRecordRepository.findById(id).orElse(null);
        if (record != null) {
            record.setIsRead(1);
            record.setAlertStatus(1);
            alertRecordRepository.save(record);
        }
    }

    @Transactional
    public void markAllAsRead() {
        alertRecordRepository.markAllAsRead();
    }

    @Transactional
    public void triggerCertAlert(SslCertInfo certInfo, DomainAsset asset) {
        List<AlertConfig> configs = getEnabledConfigs();
        if (configs.isEmpty()) return;
        String alertType = getAlertTypeForRiskLevel(certInfo.getRiskLevel());
        for (AlertConfig config : configs) {
            if (config.appliesToRiskLevel(certInfo.getRiskLevel())) {
                String subject = String.format("[%s] 证书告警 - %s",
                        getRiskLevelName(certInfo.getRiskLevel()), asset.getUrl());
                String content = buildCertAlertContent(certInfo, asset);
                // 使用 AlertUtil 已有方法发送告警
                AlertRecord record = new AlertRecord();
                record.setAssetId(asset.getId());
                record.setCertId(certInfo.getId());
                
                if (Boolean.TRUE.equals(config.getEmailEnabled())) {
                    alertUtil.sendEmailAlert(config, record, asset, certInfo);
                }
                if (Boolean.TRUE.equals(config.getWebhookEnabled())) {
                    alertUtil.sendWechatAlert(config, record, asset, certInfo);
                }
                if (Boolean.TRUE.equals(config.getDingtalkEnabled())) {
                    alertUtil.sendDingtalkAlert(config, record, asset, certInfo);
                }
                saveAlertRecord(config, certInfo, asset, alertType);
            }
        }
    }

    @Transactional
    public void triggerUrlAlert(ScanResult scanResult, DomainAsset asset) {
        List<AlertConfig> configs = getEnabledConfigs();
        if (configs.isEmpty()) return;
        for (AlertConfig config : configs) {
            String subject = String.format("[URL异常] %s", asset.getUrl());
            String content = buildUrlAlertContent(scanResult, asset);
            // URL 告警暂用简单方式记录
            saveUrlAlertRecord(config, scanResult, asset);
        }
    }

    private void saveAlertRecord(AlertConfig config, SslCertInfo certInfo, DomainAsset asset, String alertType) {
        AlertRecord record = new AlertRecord();
        record.setAlertType(alertType);
        record.setRiskLevel(certInfo.getRiskLevel());
        record.setTitle(String.format("证书[%s]: %s", getRiskLevelName(certInfo.getRiskLevel()), asset.getUrl()));
        record.setContent(String.format("证书剩余 %d 天过期\nURL: %s\n颁发机构: %s",
                certInfo.getRemainDays(), asset.getUrl(), certInfo.getIssuer()));
        record.setAssetId(asset.getId());
        record.setCertId(certInfo.getId());
        record.setAlertStatus(0);
        record.setIsRead(0);
        record.setAlertTime(LocalDateTime.now());
        record.setSendTime(LocalDateTime.now());
        alertRecordRepository.save(record);
    }

    private void saveUrlAlertRecord(AlertConfig config, ScanResult scanResult, DomainAsset asset) {
        AlertRecord record = new AlertRecord();
        record.setAlertType("URL_DOWN");
        record.setRiskLevel(0);
        record.setTitle(String.format("[URL异常] %s", asset.getUrl()));
        record.setContent(String.format("状态码: %s\n响应时间: %d ms\n错误: %s",
                scanResult.getStatusCode(), scanResult.getResponseTime(),
                scanResult.getErrorMessage() != null ? scanResult.getErrorMessage() : "无"));
        record.setAssetId(asset.getId());
        record.setAlertStatus(0);
        record.setIsRead(0);
        record.setAlertTime(LocalDateTime.now());
        record.setSendTime(LocalDateTime.now());
        alertRecordRepository.save(record);
    }

    private String buildCertAlertContent(SslCertInfo certInfo, DomainAsset asset) {
        return String.format(
                "证书告警通知\n\nURL: %s\n业务分组: %s\n负责人: %s\n风险等级: %s\n证书颁发机构: %s\n剩余天数: %d 天\n扫描时间: %s",
                asset.getUrl(), asset.getBusinessGroup(), asset.getOwner(),
                getRiskLevelName(certInfo.getRiskLevel()), certInfo.getIssuer(),
                certInfo.getRemainDays(), certInfo.getScanTime());
    }

    private String buildUrlAlertContent(ScanResult scanResult, DomainAsset asset) {
        return String.format(
                "URL异常告警通知\n\nURL: %s\n业务分组: %s\n状态码: %s\n响应时间: %d ms\n错误信息: %s",
                asset.getUrl(), asset.getBusinessGroup(),
                scanResult.getStatusCode(), scanResult.getResponseTime(),
                scanResult.getErrorMessage() != null ? scanResult.getErrorMessage() : "无");
    }

    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", alertRecordRepository.count());
        stats.put("unread", alertRecordRepository.countByIsRead(0));
        stats.put("read", alertRecordRepository.countByIsRead(1));
        return stats;
    }

    private String getAlertTypeForRiskLevel(Integer level) {
        if (level == null) return "CERT_UNKNOWN";
        switch (level) {
            case 1: return "CERT_WARNING";
            case 2: return "CERT_CRITICAL";
            case 3: return "CERT_EXPIRED";
            default: return "CERT_NORMAL";
        }
    }

    private String getRiskLevelName(Integer riskLevel) {
        if (riskLevel == null) return "未知";
        switch (riskLevel) {
            case 0: return "正常";
            case 1: return "预警";
            case 2: return "高危";
            case 3: return "已过期";
            default: return "未知";
        }
    }
}
