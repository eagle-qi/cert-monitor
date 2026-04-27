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
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) Integer riskLevel) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "validEnd"));
        Page<SslCertInfo> certs = certService.getCerts(assetId, riskLevel, pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("total", certs.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", certs.getContent());
        return result;
    }
    
    @GetMapping("/{id}")
    public SslCertInfo getById(@PathVariable Long id) {
        return certService.getCerts(id, null, PageRequest.of(0, 1)).getContent().stream().findFirst().orElse(null);
    }
    
    @GetMapping("/asset/{assetId}")
    public SslCertInfo getByAssetId(@PathVariable Long assetId) {
        return certService.getCertByAssetId(assetId);
    }
    
    @PostMapping("/scan/{assetId}")
    public Map<String, Object> scan(@PathVariable Long assetId) {
        return certService.scanCert(assetId);
    }
    
    @PostMapping("/scan-all")
    public Map<String, Object> scanAll() {
        long start = System.currentTimeMillis();
        certService.scanAllEnabled();
        long duration = System.currentTimeMillis() - start;
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "全量证书扫描已启动");
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
        Map<String, Object> result = new HashMap<>();
        result.put("riskDistribution", certService.getRiskDistribution());
        result.put("total", certService.countByRiskLevel(null));
        result.put("normal", certService.countByRiskLevel(0));
        result.put("warning", certService.countByRiskLevel(1));
        result.put("critical", certService.countByRiskLevel(2));
        result.put("expired", certService.countByRiskLevel(3));
        return result;
    }
}
