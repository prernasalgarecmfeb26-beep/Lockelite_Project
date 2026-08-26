package com.lockelite.repository;

import com.lockelite.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC")
    List<AuditLog> findAllOrderByTimestampDesc();

    Optional<AuditLog> findTopByOrderByTimestampDesc();
}
