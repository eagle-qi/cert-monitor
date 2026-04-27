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

import java.util.*;

@Service
public class AlertService {

    @Autowired
    private AlertConfigRepository alertConfigRepository;
    @Autowired
    private AlertRecordRepository alertRecordRepository;
    @Autowired
    private DomainAssetRepository domainAssetRepository;
    @Autowired
    private SslCertInfoRepository sslCertInfoRepository;
    @Autowired
    private ScanResultRepository scanResultRepository;
    @Autowired
    private AlertUtil alertUtil;

    @Transactional
    public AlertConfig createAlertConfig(AlertConfig config) {
        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());
        return alertConfigRepository.save(config);
    }

    @Transactional
    public AlertConfig updateAlertConfig(Long id, AlertConfig config) {
        AlertConfig existing = alertConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("告警配置不存在"));
        existing.setName(config.getName());
        existing.setAlertType(config.getAlertType());
        existing.setRiskLevels(config.getRiskLevels());
        existing.setEnabled(config.getEnabled());
        existing.setEmailEnabled(config.getEmailEnabled());
        existing.setEmailTo(config.getEmailTo());
        existing.setWebhookEnabled(config.getWebhookEnabled());
        existing.setWebhookUrl(config.getWebhookUrl());
        existing.setDingtalkEnabled(config.getDingtalkEnabled());
        existing.setDingtalkWebhookUrl(config.getDingtalkWebhookUrl());
        existing.setWechatEnabled(config.getWechatEnabled());
        existing.setWechatWebhookUrl(config.getWechatWebhookUrl());
        existing.setUpdateTime(new Date());
        return alertConfigRepository.save(existing);
    }

    public Page<AlertConfig> getAlertConfigs(int page, int size) {
        return alertConfigRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")));
    }

    public List<AlertConfig> getEnabledConfigs() {
        return alertConfigRepository.findByEnabled(1);
    }

    public AlertConfig getAlertConfig(Long id) {
        return alertConfigRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteAlertConfig(Long id) {
        alertConfigRepository.deleteById(id);
    }

    @Transactional
    public void triggerCertAlert(SslCertInfo certInfo, String alertType) {
        List<AlertConfig> configs = alertConfigRepository.findByEnabled(1);
        if (configs.isEmpty()) return;
        DomainAsset asset = domainAssetRepository.findById(certInfo.getAssetId()).orElse(null);
        if (asset == null) return;
        String alertKey = "cert:" + certInfo.getAssetId() + ":" + certInfo.getRiskLevel();
        if (isAlertSuppressed(alertKey)) return;
        for (AlertConfig config : configs) {
            if (config.getRiskLevels() != null && config.getRiskLevels().contains(certInfo.getRiskLevel())) {
                sendCertAlert(config, certInfo, asset, alertType);
                saveCertAlertRecord(config, certInfo, asset, alertType);
            }
        }
    }

    @Transactional
    public void triggerUrlAlert(ScanResult scanResult, String alertType) {
        List<AlertConfig> configs = alertConfigRepository.findByEnabled(1);
        if (configs.isEmpty()) return;
        DomainAsset asset = domainAssetRepository.findById(scanResult.getAssetId()).orElse(null);
        if (asset == null) return;
        String alertKey = "url:" + scanResult.getAssetId();
        if (isAlertSuppressed(alertKey)) return;
        for (AlertConfig config : configs) {
            if (config.getAlertType() == null || config.getAlertType().contains(alertType)) {
                sendUrlAlert(config, scanResult, asset, alertType);
                saveUrlAlertRecord(config, scanResult, asset, alertType);
            }
        }
    }

    private void sendCertAlert(AlertConfig config, SslCertInfo certInfo, DomainAsset asset, String alertType) {
        String subject = String.format("[%s] 证书告警 - %s", getRiskLevelName(certInfo.getRiskLevel()), asset.getUrl());
        String content = String.format(
                "证书告警通知\n\nURL: %s\n业务分组: %s\n负责人: %s\n风险等级: %s\n证书颁发机构: %s\n证书有效期: %s 至 %s\n剩余天数: %d 天\n告警类型: %s\n扫描时间: %s",
                asset.getUrl(), asset.getBusinessGroup(), asset.getOwner(),
                getRiskLevelName(certInfo.getRiskLevel()), certInfo.getIssuer(),
                certInfo.getValidStart(), certInfo.getValidEnd(),
                certInfo.getRemainDays(), alertType, certInfo.getScanTime());
        if (Boolean.TRUE.equals(config.getEmailEnabled())) alertUtil.sendEmail(config.getEmailTo(), subject, content);
        if (Boolean.TRUE.equals(config.getWebhookEnabled())) alertUtil.sendWebhook(config.getWebhookUrl(), subject, content);
        if (Boolean.TRUE.equals(config.getWechatEnabled())) alertUtil.sendWechat(config.getWechatWebhookUrl(), subject, content);
        if (Boolean.TRUE.equals(config.getDingtalkEnabled())) alertUtil.sendDingtalk(config.getDingtalkWebhookUrl(), subject, content);
    }

    private void sendUrlAlert(AlertConfig config, ScanResult scanResult, DomainAsset asset, String alertType) {
        String subject = String.format("[%s] URL异常告警 - %s",
                alertType.equals("URL_DOWN") ? "严重" : "警告", asset.getUrl());
        String content = String.format(
                "URL异常告警通知\n\nURL: %s\n业务分组: %s\n负责人: %s\n告警类型: %s\n状态码: %s\n响应时间: %d ms\n错误信息: %s\n扫描时间: %s",
                asset.getUrl(), asset.getBusinessGroup(), asset.getOwner(),
                alertType, scanResult.getStatusCode(), scanResult.getResponseTime(),
                scanResult.getErrorMessage(), scanResult.getScanTime());
        if (Boolean.TRUE.equals(config.getEmailEnabled())) alertUtil.sendEmail(config.getEmailTo(), subject, content);
        if (Boolean.TRUE.equals(config.getWebhookEnabled())) alertUtil.sendWebhook(config.getWebhookUrl(), subject, content);
        if (Boolean.TRUE.equals(config.getWechatEnabled())) alertUtil.sendWechat(config.getWechatWebhookUrl(), subject, content);
        if (Boolean.TRUE.equals(config.getDingtalkEnabled())) alertUtil.sendDingtalk(config.getDingtalkWebhookUrl(), subject, content);
    }

    private void saveCertAlertRecord(AlertConfig config, SslCertInfo certInfo, DomainAsset asset, String alertType) {
        AlertRecord record = new AlertRecord();
        record.setAlertType(alertType);
        record.setAlertLevel(certInfo.getRiskLevel());
        record.setTitle(String.format("证书[%s]: %s", getRiskLevelName(certInfo.getRiskLevel()), asset.getUrl()));
        record.setContent(String.format("证书剩余 %d 天过期\nURL: %s\n颁发机构: %s",
                certInfo.getRemainDays(), asset.getUrl(), certInfo.getIssuer()));
        record.setAssetId(asset.getId());
        record.setCertId(certInfo.getId());
        record.setAlertStatus(0);
        record.setAlertTime(new Date());
        record.setCreateTime(new Date());
        alertRecordRepository.save(record);
    }

    private void saveUrlAlertRecord(AlertConfig config, ScanResult scanResult, DomainAsset asset, String alertType) {
        AlertRecord record = new AlertRecord();
        record.setAlertType(alertType);
        record.setAlertLevel(0);
        record.setTitle(String.format("[%s] %s", alertType, asset.getUrl()));
        record.setContent(String.format("状态码: %s\n响应时间: %d ms\n错误: %s",
                scanResult.getStatusCode(), scanResult.getResponseTime(), scanResult.getErrorMessage()));
        record.setAssetId(asset.getId());
        record.setAlertStatus(0);
        record.setAlertTime(new Date());
        record.setCreateTime(new Date());
        alertRecordRepository.save(record);
    }

    private boolean isAlertSuppressed(String alertKey) {
        Long lastAlertTime = alertSuppressionMap.get(alertKey);
        long now = System.currentTimeMillis();
        if (lastAlertTime != null && (now - lastAlertTime) < 30 * 60 * 1000) {
            return true;
        }
        alertSuppressionMap.put(alertKey, now);
        return false;
    }

    private static final Map<String, Long> alertSuppressionMap = new HashMap<>();

    public Page<AlertRecord> getAlertRecords(String alertType, Integer alertStatus, Pageable pageable) {
        if (alertType != null && alertStatus != null) {
            return alertRecordRepository.findByAlertTypeAndAlertStatus(alertType, alertStatus, pageable);
        } else if (alertType != null) {
            return alertRecordRepository.findByAlertType(alertType, pageable);
        } else if (alertStatus != null) {
            return alertRecordRepository.findByAlertStatus(alertStatus, pageable);
        }
        return alertRecordRepository.findAll(pageable);
    }

    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", alertRecordRepository.count());
        stats.put("unread", alertRecordRepository.countByAlertStatus(0));
        stats.put("read", alertRecordRepository.countByAlertStatus(1));
        stats.put("certAlert", alertRecordRepository.countByAlertType("CERT_WARNING"));
        stats.put("certCritical", alertRecordRepository.countByAlertType("CERT_CRITICAL"));
        stats.put("certExpired", alertRecordRepository.countByAlertType("CERT_EXPIRED"));
        stats.put("urlDown", alertRecordRepository.countByAlertType("URL_DOWN"));
        return stats;
    }

    @Transactional
    public void markAsRead(List<Long> ids) {
        alertRecordRepository.markAsRead(ids);
    }

    @Transactional
    public void markAllAsRead() {
        alertRecordRepository.markAllAsRead();
    }

    @Transactional
    public void deleteAlertRecord(Long id) {
        alertRecordRepository.deleteById(id);
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
