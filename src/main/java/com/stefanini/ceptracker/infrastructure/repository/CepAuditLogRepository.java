package com.stefanini.ceptracker.infrastructure.repository;

import com.stefanini.ceptracker.domain.entity.CepAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CepAuditLogRepository extends JpaRepository<CepAuditLog, Long> {

    Page<CepAuditLog> findByCepOrderByRequestTimestampDesc(String cep, Pageable pageable);

    @Query("SELECT c FROM CepAuditLog c WHERE c.requestTimestamp BETWEEN :start AND :end ORDER BY c.requestTimestamp DESC")
    List<CepAuditLog> findByRequestTimestampBetween(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(c) FROM CepAuditLog c WHERE c.success = true")
    Long countSuccessfulRequests();

    @Query("SELECT COUNT(c) FROM CepAuditLog c WHERE c.success = false")
    Long countFailedRequests();
}