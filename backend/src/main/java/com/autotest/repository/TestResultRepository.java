package com.autotest.repository;

import com.autotest.entity.TestResult;
import com.autotest.entity.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    @Query("SELECT res FROM TestResult res WHERE res.testRun.id = :testRunId AND res.testRun.project.owner.id = :ownerId ORDER BY res.id")
    List<TestResult> findByTestRunIdAndOwnerId(
            @Param("testRunId") Long testRunId,
            @Param("ownerId") Long ownerId
    );

    @Query("SELECT result.status, COUNT(result) FROM TestResult result WHERE result.testRun.project.owner.id = :ownerId GROUP BY result.status")
    List<Object[]> countByStatusForOwner(@Param("ownerId") Long ownerId);

    @Query(value = """
            SELECT DATE(r.completed_at), r.status, COUNT(*)
            FROM test_results r
            JOIN test_runs tr ON tr.id = r.test_run_id
            JOIN testing_projects p ON p.id = tr.project_id
            WHERE p.owner_id = :ownerId
              AND r.completed_at >= :since
              AND r.status IN ('PASSED', 'FAILED', 'ERROR')
            GROUP BY DATE(r.completed_at), r.status
            ORDER BY DATE(r.completed_at)
            """, nativeQuery = true)
    List<Object[]> countCompletedByDayAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("since") java.sql.Timestamp since
    );

    @Query("SELECT result FROM TestResult result WHERE result.id = :resultId AND result.testRun.project.owner.id = :ownerId")
    Optional<TestResult> findByIdAndOwnerId(
            @Param("resultId") Long resultId,
            @Param("ownerId") Long ownerId
    );
}
