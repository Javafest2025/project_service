package org.solace.scholar_ai.project_service.service.project;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.project.CreateProjectDto;
import org.solace.scholar_ai.project_service.dto.project.ProjectDto;
import org.solace.scholar_ai.project_service.dto.project.UpdateProjectDto;
import org.solace.scholar_ai.project_service.mapping.project.ProjectMapper;
import org.solace.scholar_ai.project_service.model.project.Project;
import org.solace.scholar_ai.project_service.repository.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ProjectDeletionService projectDeletionService;

    /**
     * Create a new project
     */
    public ProjectDto createProject(CreateProjectDto createProjectDto, UUID userId) {
        log.info("Creating new project: {} for user: {}", createProjectDto.name(), userId);

        Project project = projectMapper.fromCreateDto(createProjectDto, userId);
        project.setLastActivity("Project created");
        Project savedProject = projectRepository.save(project);

        log.info("Project created successfully with ID: {}", savedProject.getId());
        return projectMapper.toDto(savedProject);
    }

    /**
     * Get project by ID
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public ProjectDto getProjectById(UUID projectId, UUID userId) {
        log.info("Fetching project with ID: {} for user: {}", projectId, userId);

        Project project = projectRepository
                .findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        return projectMapper.toDto(project);
    }

    /**
     * Get all projects for a user
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<ProjectDto> getProjectsByUserId(UUID userId) {
        log.info("Fetching all projects for user: {}", userId);

        List<Project> projects = projectRepository.findByUserId(userId);
        return projects.stream().map(projectMapper::toDto).toList();
    }

    /**
     * Get projects by status for a user
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<ProjectDto> getProjectsByUserIdAndStatus(UUID userId, Project.Status status) {
        log.info("Fetching projects with status: {} for user: {}", status, userId);

        List<Project> projects = projectRepository.findByUserIdAndStatus(userId, status);
        return projects.stream().map(projectMapper::toDto).toList();
    }

    /**
     * Get starred projects for a user
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<ProjectDto> getStarredProjects(UUID userId) {
        log.info("Fetching starred projects for user: {}", userId);

        List<Project> projects = projectRepository.findStarredProjectsByUserId(userId);
        return projects.stream().map(projectMapper::toDto).toList();
    }

    /**
     * Update an existing project
     */
    public ProjectDto updateProject(UUID projectId, UpdateProjectDto updateProjectDto, UUID userId) {
        log.info("Updating project with ID: {} for user: {}", projectId, userId);

        Project existingProject = projectRepository
                .findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        // Update fields from DTO
        if (updateProjectDto.name() != null) {
            existingProject.setName(updateProjectDto.name());
        }
        if (updateProjectDto.description() != null) {
            existingProject.setDescription(updateProjectDto.description());
        }
        if (updateProjectDto.domain() != null) {
            existingProject.setDomain(updateProjectDto.domain());
        }
        if (updateProjectDto.topics() != null) {
            existingProject.setTopics(projectMapper.listToString(updateProjectDto.topics()));
        }
        if (updateProjectDto.tags() != null) {
            existingProject.setTags(projectMapper.listToString(updateProjectDto.tags()));
        }
        if (updateProjectDto.status() != null) {
            existingProject.setStatus(projectMapper.stringToStatusEnum(updateProjectDto.status()));
        }
        if (updateProjectDto.progress() != null) {
            existingProject.setProgress(updateProjectDto.progress());
        }
        if (updateProjectDto.lastActivity() != null) {
            existingProject.setLastActivity(updateProjectDto.lastActivity());
        }
        if (updateProjectDto.isStarred() != null) {
            existingProject.setIsStarred(updateProjectDto.isStarred());
        }

        // Set last activity if not provided
        if (updateProjectDto.lastActivity() == null) {
            existingProject.setLastActivity("Project updated");
        }

        Project savedProject = projectRepository.save(existingProject);

        log.info("Project updated successfully with ID: {}", savedProject.getId());
        return projectMapper.toDto(savedProject);
    }

    /**
     * Delete a project and all its related data
     */
    public void deleteProject(UUID projectId, UUID userId) {
        log.info("Deleting project with ID: {} for user: {}", projectId, userId);

        // Use the comprehensive deletion service to delete project and all related data
        projectDeletionService.deleteProjectCompletely(projectId, userId);

        log.info("Project and all related data deleted successfully with ID: {}", projectId);
    }

    /**
     * Update project paper count
     */
    public void updateProjectPaperCount(UUID projectId, int totalPapers) {
        log.info("Updating paper count for project: {} to: {}", projectId, totalPapers);

        Project project =
                projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));

        project.setTotalPapers(totalPapers);
        project.setLastActivity("Paper count updated");
        projectRepository.save(project);
    }

    /**
     * Update project active tasks count
     */
    public void updateProjectActiveTasksCount(UUID projectId, int activeTasks) {
        log.info("Updating active tasks count for project: {} to: {}", projectId, activeTasks);

        Project project =
                projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));

        project.setActiveTasks(activeTasks);
        project.setLastActivity("Active tasks updated");
        projectRepository.save(project);
    }

    /**
     * Get project count by status for a user
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public long getProjectCountByStatus(UUID userId, Project.Status status) {
        return projectRepository.countByUserIdAndStatus(userId, status);
    }

    /**
     * Toggle project starred status
     */
    public ProjectDto toggleProjectStar(UUID projectId, UUID userId) {
        log.info("Toggling star status for project: {} and user: {}", projectId, userId);

        Project project = projectRepository
                .findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        project.setIsStarred(!project.getIsStarred());
        project.setLastActivity("Star status toggled");
        Project savedProject = projectRepository.save(project);

        log.info("Project star status toggled successfully for ID: {}", projectId);
        return projectMapper.toDto(savedProject);
    }
}
