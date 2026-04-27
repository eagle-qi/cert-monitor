package com.certmonitor.controller;

import com.certmonitor.entity.SslCertInfo;
import com.certmonitor.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    /**
     * 获取大盘统计数据
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return dashboardService.getDashboardStats();
    }
    
    /**
     * 获取证书风险分布（饼图数据）
     */
    @GetMapping("/cert-risk-distribution")
    public Map<String, Object> getCertRiskDistribution() {
        return Map.of("data", dashboardService.getCertRiskDistribution());
    }
    
    /**
     * 获取证书过期趋势
     */
    @GetMapping("/cert-trend")
    public Map<String, Object> getCertTrend() {
        return Map.of("data", dashboardService.getCertExpiryTrend());
    }
    
    /**
     * 获取即将过期的证书
     */
    @GetMapping("/expiring-certs")
    public Map<String, Object> getExpiringCerts(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        return Map.of(
                "days", days,
                "list", dashboardService.getExpiringCerts(days, PageRequest.of(page - 1, size))
        );
    }
    
    /**
     * 获取业务分组统计
     */
    @GetMapping("/business-groups")
    public Map<String, Object> getBusinessGroupStats() {
        return Map.of("data", dashboardService.getBusinessGroupStats());
    }
}
