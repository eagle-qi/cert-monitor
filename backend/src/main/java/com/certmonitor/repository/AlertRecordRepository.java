package com.certmonitor.repository;

import com.certmonitor.entity.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {
    
    Page<AlertRecord> findByOrderBySendTimeDesc(Pageable pageable);
    
    Page<AlertRecord> findByIsReadOrderBySendTimeDesc(Integer isRead, Pageable pageable);
    
    Page<AlertRecord> findByAlertType(String alertType, Pageable pageable);
    
    Page<AlertRecord> findByAlertStatus(Integer alertStatus, Pageable pageable);
    
    Page<AlertRecord> findByAlertTypeAndAlertStatus(String alertType, Integer alertStatus, Pageable pageable);
    
    long countByIsRead(Integer isRead);
    
    long countByAlertStatus(Integer alertStatus);
    
    long countByAlertType(String alertType);
    
    @Modifying
    @Query("UPDATE AlertRecord a SET a.isRead = 1, a.alertStatus = 1 WHERE a.isRead = 0")
    void markAllAsRead();
}
