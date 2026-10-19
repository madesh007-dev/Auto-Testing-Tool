package com.autotest.controller;

import com.autotest.dto.ProjectRequest;
import com.autotest.dto.ProjectResponse;
import com.autotest.security.UserPrincipal;
import com.autotest.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Testing Projects", description = "Endpoints for managing testing projects")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "Get paginated list of projects owned by the authenticated user")
    public ResponseEntity<Page<ProjectResponse>> getProjects(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProjectResponse> projects = projectService.getProjects(currentUser.getId(), pageable);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project details by ID")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        ProjectResponse project = projectService.getProjectById(projectId, currentUser.getId());
        return ResponseEntity.ok(project);
    }

    @PostMapping
    @Operation(summary = "Create a new testing project")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        ProjectResponse project = projectService.createProject(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update an existing testing project")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        ProjectResponse project = projectService.updateProject(projectId, request, currentUser.getId());
        return ResponseEntity.ok(project);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete an existing testing project")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        projectService.deleteProject(projectId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
