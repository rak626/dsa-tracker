# DSA Tracker - Project Context

A personal DSA problem tracker to manage and track progress on LeetCode, GeeksForGeeks, and other platform problems.

## Project Overview

- **Core Purpose**: Track DSA practice progress with analytics, revision tracking, and automated GitHub backups.
- **Tech Stack**:
    - **Backend**: Spring Boot 3.5.9, Java 21.
    - **Database**: PostgreSQL 16 (handled by Flyway migrations).
    - **Persistence**: Spring Data JPA.
    - **Security**: Spring Security (Basic Auth for admin access).
    - **UI**: Thymeleaf templates with Pico CSS and Vanilla JS.
    - **API Documentation**: SpringDoc OpenAPI (Swagger UI).
    - **Build Tool**: Maven.

## Architecture

- **Controller Layer**:
    - `ui/`: Thymeleaf-based controllers for web views.
    - `api/`: REST controllers for programmatic access and data operations.
- **Service Layer**: Business logic implementations (e.g., `QuestionServiceImpl`).
- **Repository Layer**: JPA repositories for data access.
- **Model Layer**: Entities (`Question`, `Topic`, `Pattern`) and DTOs for requests/responses.
- **Migrations**: Database schema managed via Flyway (`src/main/resources/db/migration`).

## Building and Running

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL 16 (or Docker)

### Run with Maven
```bash
./mvnw spring-boot:run
```

### Build and Package
```bash
./mvnw clean package -DskipTests
```

### Profiles
- `dev` (Default): Local development with H2 for tests and PostgreSQL for runtime. Available at `http://localhost:7008`.
- `prod`: Production-ready configuration. Available at `http://localhost:5050`.

## Testing

- **Framework**: JUnit 5, AssertJ.
- **Run all tests**: `./mvnw test`
- **Unit Tests**: Located in `src/test/java/com/rakesh/dsa/tracker/unit`.
- **Naming Convention**: `[MethodName]_[Scenario]_[ExpectedResult]` (e.g., `detectPlatform_withLeetCodeUrl_returnsLeetCode`).

## Development Conventions

- **Security**: Admin credentials must be configured via `app.security.admin-username` and `app.security.admin-password`.
- **Lombok**: Use Lombok annotations (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`) to reduce boilerplate.
- **Data Integrity**: Use Flyway for all schema changes. Do not modify existing migration files; create new ones for updates.
- **Automated Fields**: `Question` entity uses `@CreationTimestamp` and `@UpdateTimestamp`.
- **Domain Logic**: Prefer keeping core logic in the Service layer (`QuestionServiceImpl`) while using utility classes (`QuestionServiceUtil`) for stateless helper methods.
- **REST APIs**: Follow RESTful conventions. API documentation is available at `/swagger-ui.html` in `dev` profile.

## Key Files
- `pom.xml`: Project dependencies and build configuration.
- `src/main/resources/application.yaml`: Centralized configuration.
- `src/main/java/com/rakesh/dsa/tracker/config/SecurityConfig.java`: Security filter chain and auth rules.
- `src/main/java/com/rakesh/dsa/tracker/model/Question.java`: Main entity for tracked problems.
- `src/main/resources/db/migration/`: SQL scripts for database versioning.
