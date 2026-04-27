package com.certmonitor.service;

import com.certmonitor.entity.DomainAsset;
import com.certmonitor.entity.SslCertInfo;
import com.certmonitor.repository.SslCertInfoRepository;
import com.certmonitor.util.SslCertificateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CertService {
    
    @Autowired
    private SslCertInfoRepository certRepository;
    
    @Autowired
    private SslCertificateUtil sslUtil;
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private AlertService alertService;
    
    public Page<SslCertInfo> list(Pageable pageable, Integer riskLevel) {
        if (riskLevel != null) {
            return certRepository.findByRiskLevel(riskLevel, pageable);
        }
        return certRepository.findAll(pageable);
    }
    
    public SslCertInfo getById(Long id) {
        return certRepository.findById(id).orElse(null);
    }
    
    public Optional<SslCertInfo> getLatestByAssetId(Long assetId) {
        return certRepository.findTopByAssetIdOrderByScanTimeDesc(assetId);
    }
    
    /**
     * 扫描单个资产的证书
     */
    @Transactional
    public SslCertInfo scanCert(Long assetId) {
        DomainAsset asset = assetService.getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在: " + assetId);
        }
        
        String url = asset.getUrl();
        int port = url.startsWith("https") ? 443 : 80;
        String host = extractHost(url);
        
        SslCertInfo certInfo;
        if ("https".equalsIgnoreCase(asset.getProtocol())) {
            certInfo = sslUtil.getCertificateInfo(host, port);
        } else {
            certInfo = new SslCertInfo();
            certInfo.setSubject("HTTP协议无证书");
            certInfo.setRiskLevel(0);
            certInfo.setRemainDays(999);
        }
        
        certInfo.setAssetId(assetId);
        certInfo.setScanTime(LocalDateTime.now());
        
        // 检测证书变更
        checkCertChange(assetId, certInfo);
        
        return certRepository.save(certInfo);
    }
    
    /**
     * 批量扫描证书
     */
    public void scanAllCerts() {
        List<DomainAsset> assets = assetService.listEnabled();
        log.info("开始扫描 {} 个资产的证书", assets.size());
        
        for (DomainAsset asset : assets) {
            try {
                SslCertInfo cert = scanCert(asset.getId());
                
                // 触发告警
                if (cert.getRiskLevel() != null && cert.getRiskLevel() >= 1) {
                    alertService.sendCertAlert(cert, asset);
                }
            } catch (Exception e) {
                log.error("扫描资产证书失败: {}, error: {}", asset.getUrl(), e.getMessage());
            }
        }
        
        log.info("证书扫描完成");
    }
    
    /**
     * 获取即将过期的证书
     */
    public List<SslCertInfo> getExpiringCerts(int days) {
        LocalDateTime threshold = LocalDateTime.now().plusDays(days);
        return certRepository.findExpiringBefore(threshold);
    }
    
    /**
     * 获取高危证书统计
     */
    public Map<Integer, Long> getRiskLevelStats() {
        List<SslCertInfo> certs = certRepository.findAll();
        return certs.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getRiskLevel() != null ? c.getRiskLevel() : 0,
                        Collectors.counting()
                ));
    }
    
    /**
     * 获取证书统计数据
     */
    public Map<String, Object> getStats() {
        List<SslCertInfo> certs = certRepository.findAll();
        long total = certs.size();
        long normal = certs.stream().filter(c -> c.getRiskLevel() != null && c.getRiskLevel() == 0).count();
        long warning = certs.stream().filter(c -> c.getRiskLevel() != null && c.getRiskLevel() == 1).count();
        long danger = certs.stream().filter(c -> c.getRiskLevel() != null && c.getRiskLevel() == 2).count();
        long expired = certs.stream().filter(c -> c.getRiskLevel() != null && c.getRiskLevel() >= 3).count();
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", total);
        stats.put("normal", normal);
        stats.put("warning", warning);
        stats.put("danger", danger);
        stats.put("expired", expired);
        
        return stats;
    }
    
    /**
     * 检测证书变更
     */
    private void checkCertChange(Long assetId, SslCertInfo newCert) {
        if (newCert.getCertFingerprint() == null) return;
        
        List<SslCertInfo> history = certRepository.findByCertFingerprint(newCert.getCertFingerprint());
        if (history.isEmpty()) {
            // 新证书或首次扫描
            log.info("检测到新证书或证书变更: asset={}, fingerprint={}", 
                    assetId, newCert.getCertFingerprint());
        }
    }
    
    private String extractHost(String url) {
        try {
            return url.replaceAll("^https?://", "").split("/")[0].split(":")[0];
        } catch (Exception e) {
            return url;
        }
    }
}
