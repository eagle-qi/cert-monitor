package com.certmonitor.repository;

import com.certmonitor.entity.ScanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScanTaskRepository extends JpaRepository<ScanTask, Long> {
    
    List<ScanTask> findByStatus(Integer status);
    
    List<ScanTask> findByScanType(Integer scanType);
}
