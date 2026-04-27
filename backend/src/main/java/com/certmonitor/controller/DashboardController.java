package com.certmonitor.controller;

import com.certmonitor.entity.ScanResult;
import com.certmonitor.entity.SslCertInfo;
import com.certmonitor.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return dashboardService.getDashboardStats();
    }
    
    @GetMapping("/cert-trend")
    public Map<String, Object> getCertTrend() {
        Map<String, Object> result = new HashMap<>();
        result.put("data", dashboardService.getCertTrend());
        return result;
    }
    
    @GetMapping("/url-distribution")
    public Map<String, Object> getUrlDistribution() {
        Map<String, Object> result = new HashMap<>();
        result.put("data", dashboardService.getUrlStatusDistribution());
        return result;
    }
    
    @GetMapping("/risk-certs")
    public List<SslCertInfo> getTopRiskCerts() {
        return dashboardService.getTopRiskCerts();
    }
    
    @GetMapping("/recent-failed")
    public List<ScanResult> getRecentFailedScans() {
        return dashboardService.getRecentFailedScans();
    }
}
