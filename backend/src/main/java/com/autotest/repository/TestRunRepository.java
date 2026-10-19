package com.autotest.repository;

import com.autotest.entity.ExecutionStatus;
import com.autotest.entity.TestRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    @Query("SELECT tr FROM TestRun tr WHERE tr.project.id = :projectId AND tr.project.owner.id = :ownerId")
    Page<TestRun> findByProjectIdAndOwnerId(
            @Param("projectId") Long projectId,
            @Param("ownerId") Long ownerId,
            Pageable pageable
    );

    @Query("SELECT tr FROM TestRun tr WHERE tr.id = :testRunId AND tr.project.owner.id = :ownerId")
    Optional<TestRun> findByIdAndOwnerId(
            @Param("testRunId") Long testRunId,
            @Param("ownerId") Long ownerId
    );

    @Query("SELECT tr FROM TestRun tr WHERE tr.project.owner.id = :ownerId")
    Page<TestRun> findAllByOwnerId(
            @Param("ownerId") Long ownerId,
            Pageable pageable
    );

    @Query("""
            SELECT tr FROM TestRun tr
            WHERE tr.project.owner.id = :ownerId
              AND (:projectId IS NULL OR tr.project.id = :projectId)
              AND (:status IS NULL OR tr.status = :status)
              AND (:from IS NULL OR tr.startedAt >= :from)
              AND (:to IS NULL OR tr.startedAt <= :to)
            """)
    Page<TestRun> findHistoryByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("projectId") Long projectId,
            @Param("status") ExecutionStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    long countByProjectOwnerId(Long ownerId);
}
