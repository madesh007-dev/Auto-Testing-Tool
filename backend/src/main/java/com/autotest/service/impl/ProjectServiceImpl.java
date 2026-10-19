package com.autotest.service.impl;

import com.autotest.dto.ProjectRequest;
import com.autotest.dto.ProjectResponse;
import com.autotest.entity.TestingProject;
import com.autotest.entity.User;
import com.autotest.exception.ResourceNotFoundException;
import com.autotest.mapper.ProjectMapper;
import com.autotest.repository.TestingProjectRepository;
import com.autotest.repository.UserRepository;
import com.autotest.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final TestingProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    public ProjectServiceImpl(
            TestingProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectMapper projectMapper
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getProjects(Long currentUserId, Pageable pageable) {
        return projectRepository.findByOwnerId(currentUserId, pageable)
                .map(projectMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, Long currentUserId) {
        TestingProject project = projectRepository.findByIdAndOwnerId(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest request, Long currentUserId) {
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        TestingProject project = new TestingProject(
                request.name(),
                request.description(),
                request.baseUrl(),
                owner
        );

        TestingProject savedProject = projectRepository.save(project);
        return projectMapper.toResponse(savedProject);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request, Long currentUserId) {
        TestingProject project = projectRepository.findByIdAndOwnerId(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        project.setName(request.name());
        project.setDescription(request.description());
        project.setBaseUrl(request.baseUrl());

        TestingProject updatedProject = projectRepository.save(project);
        return projectMapper.toResponse(updatedProject);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Long currentUserId) {
        TestingProject project = projectRepository.findByIdAndOwnerId(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        projectRepository.delete(project);
    }
}
