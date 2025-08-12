package org.solace.scholar_ai.project_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Swagger API documentation for the Project Service.
 * This configuration provides comprehensive API documentation for project
 * management
 * operations including CRUD operations, project statistics, and project
 * actions.
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8083}")
    private String serverPort;

    /**
     * Creates the main OpenAPI configuration for the project service.
     * This service handles all project-related operations including creation,
     * updates, deletion, and project statistics.
     *
     * @return OpenAPI configuration for project service
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ScholarAI Project Service API")
                        .description(
                                """
                                        ## ScholarAI Project Service API Documentation

                                        This API provides comprehensive project management services for the ScholarAI platform.

                                        ### 📋 Project Management
                                        The project service handles all aspects of research project management:
                                        - Create, read, update, and delete projects
                                        - Project status management (Active, Paused, Completed, Archived)
                                        - Project progress tracking
                                        - Project statistics and analytics
                                        - Star/unstar projects for quick access

                                        ### 🔧 Core Features
                                        - **Project CRUD Operations**: Full lifecycle management of research projects
                                        - **Status Management**: Track project progress through different states
                                        - **Progress Tracking**: Monitor project completion percentage
                                        - **Paper Management**: Track total papers and active tasks
                                        - **User-specific Projects**: All operations are scoped to specific users
                                        - **Project Statistics**: Get comprehensive project analytics

                                        ### 🚀 Quick Start for Developers
                                        1. The service runs on port 8083 by default
                                        2. All endpoints require a `userId` parameter for authorization
                                        3. Project operations are user-scoped for security
                                        4. Use the provided DTOs for structured data exchange

                                        ### 📋 Available Endpoints

                                        #### Project CRUD Operations
                                        - `POST /api/v1/projects` - Create a new project
                                        - `GET /api/v1/projects/{projectId}` - Get project by ID
                                        - `GET /api/v1/projects` - Get all projects for a user
                                        - `PUT /api/v1/projects/{projectId}` - Update an existing project
                                        - `DELETE /api/v1/projects/{projectId}` - Delete a project

                                        #### Project Queries
                                        - `GET /api/v1/projects/status/{status}` - Get projects by status
                                        - `GET /api/v1/projects/starred` - Get starred projects
                                        - `GET /api/v1/projects/stats` - Get project statistics

                                        #### Project Actions
                                        - `POST /api/v1/projects/{projectId}/toggle-star` - Toggle project star status
                                        - `PUT /api/v1/projects/{projectId}/paper-count` - Update project paper count
                                        - `PUT /api/v1/projects/{projectId}/active-tasks` - Update project active tasks count

                                        ### 🔐 Authentication & Authorization
                                        - All endpoints require a `userId` parameter
                                        - Projects are user-scoped for security
                                        - No cross-user project access is allowed
                                        - User ID validation ensures data isolation

                                        ### 📊 Data Models

                                        #### Project Status Values
                                        - `ACTIVE` - Project is currently being worked on
                                        - `PAUSED` - Project is temporarily paused
                                        - `COMPLETED` - Project has been completed
                                        - `ARCHIVED` - Project has been archived

                                        #### Project Fields
                                        - **Basic Info**: name, description, domain
                                        - **Categorization**: topics, tags
                                        - **Progress**: status, progress percentage
                                        - **Metrics**: totalPapers, activeTasks
                                        - **Metadata**: createdAt, updatedAt, lastActivity
                                        - **User Preferences**: isStarred

                                        ### 📝 Usage Examples

                                        #### Create a New Project
                                        ```
                                        POST /api/v1/projects
                                        {
                                          "userId": "550e8400-e29b-41d4-a716-446655440001",
                                          "name": "AI in Healthcare Research",
                                          "description": "Comprehensive research on AI applications in healthcare",
                                          "domain": "Computer Vision",
                                          "topics": ["machine learning", "neural networks"],
                                          "tags": ["healthcare", "AI", "research"]
                                        }
                                        ```

                                        #### Get Projects by Status
                                        ```
                                        GET /api/v1/projects/status/ACTIVE?userId=550e8400-e29b-41d4-a716-446655440001
                                        ```

                                        #### Update Project Progress
                                        ```
                                        PUT /api/v1/projects/{projectId}
                                        {
                                          "userId": "550e8400-e29b-41d4-a716-446655440001",
                                          "progress": 75,
                                          "status": "ACTIVE",
                                          "lastActivity": "2 hours ago"
                                        }
                                        ```

                                        #### Get Project Statistics
                                        ```
                                        GET /api/v1/projects/stats?userId=550e8400-e29b-41d4-a716-446655440001
                                        ```

                                        ### 🔧 Configuration
                                        The service uses the following configuration:
                                        - **Database**: PostgreSQL for project data storage
                                        - **Validation**: Jakarta Bean Validation for data integrity
                                        - **Serialization**: Jackson for JSON processing
                                        - **Documentation**: OpenAPI 3.0 with Swagger UI
                                        - **Error Handling**: Standardized API responses with error codes

                                        ### 📈 Response Format
                                        All endpoints return standardized API responses:
                                        ```json
                                        {
                                          "status": 200,
                                          "message": "Success message",
                                          "data": { ... },
                                          "timestamp": "2024-01-15T10:30:00Z"
                                        }
                                        ```

                                        ### 🚨 Error Handling
                                        - **400 Bad Request**: Invalid input data or validation errors
                                        - **404 Not Found**: Project not found or access denied
                                        - **500 Internal Server Error**: Unexpected server errors

                                        ### 🔍 Monitoring & Health
                                        - Health checks available at `/actuator/health`
                                        - Metrics available at `/actuator/metrics`
                                        - Service information at `/actuator/info`
                                        """)
                        .version("1.0")
                        .contact(
                                new Contact().name("ScholarAI Development Team").email("dev@scholarai.com"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Development Server"),
                        new Server().url("https://project.scholarai.com").description("Production Server")));
    }
}
