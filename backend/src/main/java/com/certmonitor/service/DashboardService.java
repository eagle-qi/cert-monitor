package com.certmonitor.service;

import com.certmonitor.entity.SslCertInfo;
import com.certmonitor.repository.SslCertInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardService {
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private ScanService scanService;
    
    @Autowired
    private CertService certService;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private SslCertInfoRepository certRepository;
    
    /**
     * 获取大盘统计数据
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 资产统计
        stats.put("totalAssets", assetService.countEnabled());
        
        // 扫描统计
        Map<String, Object> scanStats = scanService.getStats();
        stats.put("accessibleRate", scanStats.get("accessibleRate"));
        stats.put("failed24h", scanStats.get("failed24h"));
        
        // 证书统计
        Map<String, Object> certStats = certService.getStats();
        stats.put("totalCerts", certStats.get("total"));
        stats.put("normalCerts", certStats.get("normal"));
        stats.put("warningCerts", certStats.get("warning"));
        stats.put("dangerCerts", certStats.get("danger"));
        stats.put("expiredCerts", certStats.get("expired"));
        
        // 告警统计
        stats.put("unreadAlerts", alertService.getUnreadCount());
        
        return stats;
    }
    
    /**
     * 获取证书风险分布（饼图数据）
     */
    public List<Map<String, Object>> getCertRiskDistribution() {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> certStats = certService.getStats();
        
        data.add(createPieItem("正常 (>30天)", (long) certStats.get("normal"), "#67C23A"));
        data.add(createPieItem("预警 (15-30天)", (long) certStats.get("warning"), "#E6A23C"));
        data.add(createPieItem("高危 (1-15天)", (long) certStats.get("danger"), "#F56C6C"));
        data.add(createPieItem("已过期", (long) certStats.get("expired"), "#909399"));
        
        return data;
    }
    
    /**
     * 获取证书过期趋势数据
     */
    public List<Map<String, Object>> getCertExpiryTrend() {
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 模拟最近7天的趋势数据
        for (int i = 6; i >= 0; i--) {
            Map<String, Object> point = new HashMap<>();
            LocalDateTime date = now.minusDays(i);
            point.put("date", date.toLocalDate().toString());
            point.put("warning", (long) (Math.random() * 10 + 5));
            point.put("danger", (long) (Math.random() * 5 + 1));
            point.put("expired", (long) (Math.random() * 3));
            data.add(point);
        }
        
        return data;
    }
    
    /**
     * 获取即将过期的证书列表
     */
    public List<SslCertInfo> getExpiringCerts(int days, Pageable pageable) {
        List<SslCertInfo> allExpiring = certService.getExpiringCerts(days);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allExpiring.size());
        
        if (start >= allExpiring.size()) {
            return Collections.emptyList();
        }
        
        return allExpiring.subList(start, end);
    }
    
    /**
     * 获取业务分组统计
     */
    public List<Map<String, Object>> getBusinessGroupStats() {
        List<Map<String, Object>> data = new ArrayList<>();
        
        List<String> groups = assetService.getBusinessGroups();
        for (String group : groups) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", group);
            item.put("assets", assetService.listEnabled().stream()
                    .filter(a -> group.equals(a.getBusinessGroup()))
                    .count());
            data.add(item);
        }
        
        return data;
    }
    
    private Map<String, Object> createPieItem(String name, long value, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        item.put("color", color);
        return item;
    }
}
