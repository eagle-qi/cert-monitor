package com.certmonitor.repository;

import com.certmonitor.entity.DomainAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DomainAssetRepository extends JpaRepository<DomainAsset, Long> {
    
    List<DomainAsset> findByStatus(Integer status);
    
    List<DomainAsset> findByIsWhitelist(Integer isWhitelist);
    
    Page<DomainAsset> findByStatusAndIsWhitelist(Integer status, Integer isWhitelist, Pageable pageable);
    
    Page<DomainAsset> findByBusinessGroup(String businessGroup, Pageable pageable);
    
    @Query("SELECT DISTINCT d.businessGroup FROM DomainAsset d WHERE d.businessGroup IS NOT NULL")
    List<String> findDistinctBusinessGroups();
    
    long countByStatus(Integer status);
    
    @Query("SELECT COUNT(d) FROM DomainAsset d WHERE d.isWhitelist = 1")
    long countWhitelistAssets();
}
