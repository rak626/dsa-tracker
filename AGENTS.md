# dsa-tracker

## Tech Stack
- Spring Boot 3.5.9, Java 21, PostgreSQL 16
- Flyway migrations, Thymeleaf UI, JPA/Hibernate
- Lombok for code generation
- Spring Security (basic auth)
- SpringDoc OpenAPI (Swagger UI)
- Spring Boot Actuator
- Chart.js for analytics charts

## Development Commands
- `./mvnw spring-boot:run` — start dev server (port 7008)
- `./mvnw test` — run tests (requires H2 in-memory DB)
- `./mvnw clean package -DskipTests` — build JAR

## Profiles
- `dev` (default) — port 7008, debug logging, Swagger enabled
- `prod` — port 5050, info logging, Swagger disabled

## Testing
- Unit tests: `src/test/java/.../unit/` (QuestionServiceUtilTest, DateFilterEnumTest)
- Integration tests: `src/test/java/.../integration/` (QuestionControllerTest)
- Tests use H2 in-memory database (`application.yaml` in test resources)

## Database
- Local: `localhost:5432`, db=`dsa_tracker`, user=`rakesh`, pass=`rakesh123_admin`
- Migrations: `src/main/resources/db/migration/V{1,2,3}__*.sql`
- `ddl-auto: validate` — migrations must be run before app starts

## Security
- Basic auth with in-memory user (configured in `application-{profile}.yaml`)
- Public endpoints: `/`, `/questions`, `/dashboard`, `/api/suggestions/**`, `/swagger-ui/**`, `/actuator/health`
- All other endpoints require authentication
- Credentials: `app.security.admin-username` and `app.security.admin-password`

## Configuration
- `application.yaml` — base config (activates dev profile)
- `application-dev.yaml` — dev profile (port 7008)
- `application-prod.yaml` — prod profile (port 5050)
- `GITHUB_BACKUP_TOKEN` env var for GitHub backup

## Project Structure
```
src/main/java/com/rakesh/dsa/tracker/
├── DsaTrackerApplication.java       # entrypoint
├── config/
│   ├── AppConfig.java               # RestTemplate bean
│   ├── SecurityConfig.java          # Spring Security + auth
│   └── GitHubHealthIndicator.java   # Health check
├── controller/
│   ├── ui/                         # Thymeleaf controllers
│   └── api/                        # REST controllers
├── service/                        # business logic
│   ├── QuestionService.java
│   ├── StatsService.java           # analytics
│   └── BackupService.java
├── repository/                     # JPA repositories
├── model/                          # entities + DTOs
├── github/                        # GitHub backup via REST API
└── schedule/                       # BackupScheduler (10 AM, 1 PM daily)
```

## Docker
- `docker-compose.yml` — postgres only
- `docker-compose.yml.local` — postgres + app container (prod profile, port 5050)
- `Dockerfile` — multi-stage build (Maven → Eclipse Temurin JRE)

## API Documentation
- Swagger UI: `http://localhost:7008/swagger-ui.html` (dev only)
- API Docs JSON: `http://localhost:7008/api-docs`

## Actuator Endpoints
- `/actuator/health` — health check (includes GitHub connectivity)
- `/actuator/info` — application info
- `/actuator/metrics` — application metrics
- `/actuator/prometheus` — Prometheus metrics
