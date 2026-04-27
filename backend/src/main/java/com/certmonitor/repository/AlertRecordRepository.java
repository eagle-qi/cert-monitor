package com.certmonitor.repository;

import com.certmonitor.entity.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {
    
    Page<AlertRecord> findByOrderBySendTimeDesc(Pageable pageable);
    
    Page<AlertRecord> findByIsReadOrderBySendTimeDesc(Integer isRead, Pageable pageable);
    
    long countByIsRead(Integer isRead);
    
    List<AlertRecord> findByAssetIdAndSendTimeAfter(Long assetId, LocalDateTime since);
    
    void deleteBySendTimeBefore(LocalDateTime before);
}
