package com.certmonitor.repository;

import com.certmonitor.entity.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {
    
    Page<AlertRecord> findByAlertType(String alertType, Pageable pageable);
    
    Page<AlertRecord> findByAlertStatus(Integer alertStatus, Pageable pageable);
    
    Page<AlertRecord> findByAlertTypeAndAlertStatus(String alertType, Integer alertStatus, Pageable pageable);
    
    long countByAlertStatus(Integer alertStatus);
    
    long countByAlertType(String alertType);
    
    @Modifying
    @Query("UPDATE AlertRecord a SET a.alertStatus = 1 WHERE a.id IN :ids")
    void markAsRead(@Param("ids") List<Long> ids);
    
    @Modifying
    @Query("UPDATE AlertRecord a SET a.alertStatus = 1 WHERE a.alertStatus = 0")
    void markAllAsRead();
}
