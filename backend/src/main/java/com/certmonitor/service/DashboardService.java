package com.certmonitor.service;

import com.certmonitor.entity.*;
import com.certmonitor.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class DashboardService {

    @Autowired
    private DomainAssetRepository domainAssetRepository;
    @Autowired
    private SslCertInfoRepository sslCertInfoRepository;
    @Autowired
    private AlertRecordRepository alertRecordRepository;
    @Autowired
    private ScanResultRepository scanResultRepository;
    @Autowired
    private AssetService assetService;
    @Autowired
    private AlertService alertService;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAssets", domainAssetRepository.count());
        stats.put("enabledAssets", assetService.countEnabled());
        stats.put("disabledAssets", assetService.countDisabled());
        stats.put("normalCerts", sslCertInfoRepository.countByRiskLevel(0));
        stats.put("warningCerts", sslCertInfoRepository.countByRiskLevel(1));
        stats.put("criticalCerts", sslCertInfoRepository.countByRiskLevel(2));
        stats.put("expiredCerts", sslCertInfoRepository.countByRiskLevel(3));
        stats.put("unreadAlerts", alertService.getUnreadCount());
        stats.put("accessibleUrls", scanResultRepository.countByIsAccessible(1));
        stats.put("unaccessibleUrls", scanResultRepository.countByIsAccessible(0));
        return stats;
    }

    public List<Map<String, Object>> getCertTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        // 简化：返回近7天的过期证书统计
        for (int i = 30; i >= 0; i--) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", java.time.LocalDate.now().minusDays(i).toString());
            day.put("expired", sslCertInfoRepository.countByRiskLevel(3));
            trend.add(day);
        }
        return trend;
    }

    public List<Map<String, Object>> getUrlStatusDistribution() {
        List<Map<String, Object>> dist = new ArrayList<>();
        Map<String, Object> accessible = new HashMap<>();
        accessible.put("name", "可访问");
        accessible.put("value", scanResultRepository.countByIsAccessible(1));
        dist.add(accessible);
        Map<String, Object> unaccessible = new HashMap<>();
        unaccessible.put("name", "不可访问");
        unaccessible.put("value", scanResultRepository.countByIsAccessible(0));
        dist.add(unaccessible);
        return dist;
    }

    public List<SslCertInfo> getTopRiskCerts() {
        return sslCertInfoRepository.findTopRiskCerts(PageRequest.of(0, 10));
    }

    public List<ScanResult> getRecentFailedScans() {
        return scanResultRepository.findRecentFailedScans(PageRequest.of(0, 20));
    }
}
