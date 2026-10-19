package com.autotest.repository;

import com.autotest.entity.TestingProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestingProjectRepository extends JpaRepository<TestingProject, Long> {
    Page<TestingProject> findByOwnerId(Long ownerId, Pageable pageable);
    Optional<TestingProject> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByIdAndOwnerId(Long id, Long ownerId);
    long countByOwnerId(Long ownerId);
}
