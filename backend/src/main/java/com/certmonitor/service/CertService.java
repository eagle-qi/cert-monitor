package com.certmonitor.service;

import com.certmonitor.entity.*;
import com.certmonitor.repository.*;
import com.certmonitor.util.SslCertificateUtil;
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
public class CertService {

    @Autowired
    private SslCertInfoRepository sslCertInfoRepository;
    @Autowired
    private DomainAssetRepository domainAssetRepository;
    @Autowired
    private SslCertificateUtil sslCertificateUtil;
    @Autowired
    private AlertService alertService;

    public Page<SslCertInfo> getCerts(Long assetId, Integer riskLevel, Pageable pageable) {
        if (assetId != null) {
            return sslCertInfoRepository.findByAssetId(assetId, pageable);
        }
        if (riskLevel != null) {
            return sslCertInfoRepository.findByRiskLevel(riskLevel, pageable);
        }
        return sslCertInfoRepository.findAll(pageable);
    }

    public SslCertInfo getCertByAssetId(Long assetId) {
        return sslCertInfoRepository.findTopByAssetIdOrderByScanTimeDesc(assetId);
    }

    public Map<String, Object> scanCert(Long assetId) {
        DomainAsset asset = domainAssetRepository.findById(assetId).orElse(null);
        if (asset == null) throw new RuntimeException("资产不存在");
        return scanCertForAsset(asset);
    }

    @Transactional
    public Map<String, Object> scanCertForAsset(DomainAsset asset) {
        Map<String, Object> result = new HashMap<>();
        SslCertInfo certInfo;
        if ("https".equalsIgnoreCase(asset.getProtocol())) {
            certInfo = sslCertificateUtil.getCertificateInfo(asset.getDomain(), 443);
        } else {
            certInfo = new SslCertInfo();
            certInfo.setRiskLevel(0);
            result.put("message", "非HTTPS协议，跳过证书扫描");
        }
        certInfo.setAssetId(asset.getId());
        certInfo.setScanTime(LocalDateTime.now());
        sslCertInfoRepository.save(certInfo);
        result.put("certInfo", certInfo);
        result.put("asset", asset);
        if (certInfo.getRiskLevel() != null && certInfo.getRiskLevel() >= 1) {
            alertService.triggerCertAlert(certInfo, asset);
        }
        return result;
    }

    public void scanAllEnabled() {
        List<DomainAsset> assets = domainAssetRepository.findByStatus(1);
        for (DomainAsset asset : assets) {
            try {
                scanCertForAsset(asset);
            } catch (Exception e) {
                // log error but continue
            }
        }
    }

    public List<SslCertInfo> getExpiringCerts(int days) {
        return sslCertInfoRepository.findExpiringCerts(days);
    }

    public long countByRiskLevel(Integer riskLevel) {
        if (riskLevel == null) return sslCertInfoRepository.count();
        return sslCertInfoRepository.countByRiskLevel(riskLevel);
    }

    public Map<String, Long> getRiskDistribution() {
        Map<String, Long> dist = new HashMap<>();
        dist.put("normal", sslCertInfoRepository.countByRiskLevel(0));
        dist.put("warning", sslCertInfoRepository.countByRiskLevel(1));
        dist.put("critical", sslCertInfoRepository.countByRiskLevel(2));
        dist.put("expired", sslCertInfoRepository.countByRiskLevel(3));
        return dist;
    }
}
