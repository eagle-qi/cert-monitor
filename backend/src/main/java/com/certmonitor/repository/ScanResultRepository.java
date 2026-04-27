package com.certmonitor.repository;

import com.certmonitor.entity.ScanResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScanResultRepository extends JpaRepository<ScanResult, Long> {
    
    Optional<ScanResult> findTopByAssetIdOrderByScanTimeDesc(Long assetId);
    
    Page<ScanResult> findByAssetIdOrderByScanTimeDesc(Long assetId, Pageable pageable);
    
    List<ScanResult> findByAssetIdAndScanTimeBetween(Long assetId, LocalDateTime start, LocalDateTime end);
    
    long countByIsAccessible(Integer isAccessible);
    
    @Query("SELECT COUNT(s) FROM ScanResult s WHERE s.isAccessible = 0 AND s.scanTime >= :since")
    long countFailedScansSince(LocalDateTime since);
    
    @Query("SELECT AVG(s.responseTime) FROM ScanResult s WHERE s.assetId = :assetId AND s.scanTime >= :since")
    Double avgResponseTimeSince(Long assetId, LocalDateTime since);
}
