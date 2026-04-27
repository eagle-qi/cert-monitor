package com.certmonitor.repository;

import com.certmonitor.entity.AlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertConfigRepository extends JpaRepository<AlertConfig, Long> {
    
    List<AlertConfig> findByEnabled(Integer enabled);
    
    Optional<AlertConfig> findByAlertType(String alertType);
}
