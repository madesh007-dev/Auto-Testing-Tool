package com.autotest.mapper;

import com.autotest.dto.ProjectResponse;
import com.autotest.entity.TestingProject;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(TestingProject project) {
        if (project == null) {
            return null;
        }

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getBaseUrl(),
                project.getOwner() != null ? project.getOwner().getId() : null,
                project.getOwner() != null ? project.getOwner().getUsername() : null,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
