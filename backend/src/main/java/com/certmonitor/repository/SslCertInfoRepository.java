package com.certmonitor.repository;

import com.certmonitor.entity.SslCertInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SslCertInfoRepository extends JpaRepository<SslCertInfo, Long> {
    
    Optional<SslCertInfo> findTopByAssetIdOrderByScanTimeDesc(Long assetId);
    
    Page<SslCertInfo> findByAssetId(Long assetId, Pageable pageable);
    
    List<SslCertInfo> findByAssetId(Long assetId);
    
    Page<SslCertInfo> findByRiskLevel(Integer riskLevel, Pageable pageable);
    
    List<SslCertInfo> findByRiskLevelIn(List<Integer> riskLevels);
    
    @Query("SELECT s FROM SslCertInfo s WHERE s.validEnd <= :date AND s.riskLevel >= 2")
    List<SslCertInfo> findExpiringBefore(LocalDateTime date);
    
    List<SslCertInfo> findExpiringCerts(int days);
    
    long countByRiskLevel(Integer riskLevel);
    
    @Query("SELECT COUNT(s) FROM SslCertInfo s WHERE s.riskLevel = 2 OR s.riskLevel = 3")
    long countHighRiskCerts();
    
    List<SslCertInfo> findByCertFingerprint(String fingerprint);
    
    @Query("SELECT s FROM SslCertInfo s WHERE s.riskLevel >= 1 ORDER BY s.riskLevel DESC, s.remainDays ASC")
    List<SslCertInfo> findTopRiskCerts(Pageable pageable);
}
