package com.autotest.service;

import com.autotest.dto.ProjectRequest;
import com.autotest.dto.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    Page<ProjectResponse> getProjects(Long currentUserId, Pageable pageable);
    ProjectResponse getProjectById(Long projectId, Long currentUserId);
    ProjectResponse createProject(ProjectRequest request, Long currentUserId);
    ProjectResponse updateProject(Long projectId, ProjectRequest request, Long currentUserId);
    void deleteProject(Long projectId, Long currentUserId);
}
