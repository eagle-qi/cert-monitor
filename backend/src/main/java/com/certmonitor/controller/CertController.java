package com.certmonitor.controller;

import com.certmonitor.entity.SslCertInfo;
import com.certmonitor.service.CertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/certs")
@CrossOrigin(origins = "*")
public class CertController {
    
    @Autowired
    private CertService certService;
    
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer riskLevel) {
        
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "validEnd"));
        Page<SslCertInfo> certs = certService.list(pageable, riskLevel);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", certs.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", certs.getContent());
        
        return result;
    }
    
    @GetMapping("/{id}")
    public SslCertInfo getById(@PathVariable Long id) {
        return certService.getById(id);
    }
    
    @GetMapping("/asset/{assetId}")
    public SslCertInfo getByAssetId(@PathVariable Long assetId) {
        return certService.getLatestByAssetId(assetId).orElse(null);
    }
    
    @PostMapping("/scan/{assetId}")
    public SslCertInfo scan(@PathVariable Long assetId) {
        return certService.scanCert(assetId);
    }
    
    @PostMapping("/scan-all")
    public Map<String, Object> scanAll() {
        long start = System.currentTimeMillis();
        certService.scanAllCerts();
        long duration = System.currentTimeMillis() - start;
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("duration", duration);
        return result;
    }
    
    @GetMapping("/expiring")
    public Map<String, Object> getExpiringCerts(@RequestParam(defaultValue = "30") int days) {
        Map<String, Object> result = new HashMap<>();
        result.put("days", days);
        result.put("list", certService.getExpiringCerts(days));
        return result;
    }
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return certService.getStats();
    }
    
    @GetMapping("/risk-distribution")
    public Map<String, Object> getRiskDistribution() {
        Map<String, Object> result = new HashMap<>();
        result.put("data", certService.getRiskLevelStats());
        return result;
    }
}
