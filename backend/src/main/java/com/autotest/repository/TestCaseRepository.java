package com.autotest.repository;

import com.autotest.entity.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    @Query("SELECT tc FROM TestCase tc WHERE tc.project.id = :projectId AND tc.project.owner.id = :ownerId")
    Page<TestCase> findByProjectIdAndOwnerId(
            @Param("projectId") Long projectId,
            @Param("ownerId") Long ownerId,
            Pageable pageable
    );

    @Query("SELECT tc FROM TestCase tc WHERE tc.project.id = :projectId AND tc.project.owner.id = :ownerId")
    List<TestCase> findAllByProjectIdAndOwnerId(
            @Param("projectId") Long projectId,
            @Param("ownerId") Long ownerId
    );

    @Query("SELECT tc FROM TestCase tc WHERE tc.id = :testCaseId AND tc.project.owner.id = :ownerId")
    Optional<TestCase> findByIdAndOwnerId(
            @Param("testCaseId") Long testCaseId,
            @Param("ownerId") Long ownerId
    );

    long countByProjectId(Long projectId);

    @Query("SELECT COUNT(tc) FROM TestCase tc WHERE tc.project.owner.id = :ownerId")
    long countByOwnerId(@Param("ownerId") Long ownerId);
}
