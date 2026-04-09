# DSA Tracker

A personal DSA problem tracker to track your LeetCode/GFG practice progress with analytics dashboard.

## Features

- **Question Management** - Add, edit, delete DSA problems with metadata
- **Topic & Pattern Tracking** - Organize problems by topics and patterns
- **Analytics Dashboard** - Visualize your progress with charts
- **Revision Tracking** - Track how many times you've revised each problem
- **GitHub Backup** - Automatic daily backups to GitHub
- **Excel Export** - Export all questions to Excel
- **REST API** - Full REST API for programmatic access
- **API Documentation** - Swagger UI for API exploration
- **Security** - Basic authentication for admin access

## Tech Stack

- **Backend**: Spring Boot 3.5.9, Java 21
- **Database**: PostgreSQL 16 with Flyway migrations
- **UI**: Thymeleaf templates with Pico CSS
- **Charts**: Chart.js
- **API Docs**: SpringDoc OpenAPI (Swagger UI)

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16 (or Docker)

## Local Development Setup

### 1. Database Setup

Start PostgreSQL using Docker:
```bash
docker-compose up -d postgres
```

Or configure your local PostgreSQL:
- Database: `dsa_tracker`
- Username: `rakesh`
- Password: `rakesh123_admin`
- Port: `5432`

### 2. Configure Credentials

Edit `src/main/resources/application-dev.yaml`:
```yaml
app:
  security:
    admin-username: your-username
    admin-password: your-secure-password
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The application will be available at: http://localhost:7008

### Default Credentials (Development)

- Username: `admin`
- Password: `admin123`

**Important**: Change these in production!

## Profiles

The application supports Spring profiles:

| Profile | Port | Description |
|---------|------|-------------|
| `dev` (default) | 7008 | Development with debug logging, Swagger UI enabled |
| `prod` | 5050 | Production with minimal logging, Swagger disabled |

Run with specific profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `GITHUB_BACKUP_TOKEN` | GitHub Personal Access Token for backups | - |
| `ADMIN_USERNAME` | Admin username (prod) | `admin` |
| `ADMIN_PASSWORD` | Admin password (prod) | - |

## API Endpoints

### Public Endpoints (No Auth Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Home page |
| GET | `/questions` | Question list |
| GET | `/dashboard` | Analytics dashboard |
| GET | `/api/suggestions/topics?q=` | Search topics |
| GET | `/api/suggestions/patterns?q=` | Search patterns |

### Protected Endpoints (Auth Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/questions` | Create question |
| POST | `/questions/{id}` | Update question |
| GET | `/questions/{id}/solve` | Increment solve count |
| GET | `/questions/{id}/revise` | Increment revise count |
| POST | `/questions/{id}/delete` | Delete question |
| GET | `/api/questions` | List all questions (API) |
| POST | `/api/questions` | Create question (API) |
| PUT | `/api/questions/{id}/update` | Update question (API) |
| DELETE | `/api/questions/{id}` | Delete question (API) |
| GET | `/api/ops/export/excel` | Export to Excel |
| GET | `/api/ops/backup` | Trigger GitHub backup |

### API Documentation

Swagger UI: http://localhost:7008/swagger-ui.html
API Docs JSON: http://localhost:7008/api-docs

## Database Schema

### Tables

- `questions` - Main question table with metadata
- `topics` - Topic reference table
- `patterns` - Pattern reference table
- `question_topics` - Many-to-many relationship
- `question_patterns` - Many-to-many relationship

### Question Fields

| Field | Type | Description |
|-------|------|-------------|
| id | BIGSERIAL | Primary key |
| problem_name | VARCHAR | Question name |
| problem_link | TEXT | URL to problem |
| platform | VARCHAR | LEETCODE, GFG, etc. |
| difficulty | VARCHAR | EASY, MEDIUM, HARD |
| video_id | VARCHAR | Optional video reference |
| solve_count | INTEGER | Times solved |
| revise_count | INTEGER | Times revised |
| last_attempted_at | TIMESTAMPTZ | Last activity timestamp |
| created_at | TIMESTAMPTZ | Creation timestamp |
| updated_at | TIMESTAMPTZ | Last update timestamp |

## Docker Deployment

### Build the Application

```bash
./mvnw clean package -DskipTests
docker build -t dsa-tracker-app:latest .
```

### Run with Docker Compose

```bash
docker-compose -f docker-compose.yml.local up -d
```

The application will be available at: http://localhost:5050

### Production Environment Variables

```bash
export ADMIN_USERNAME=your-admin-user
export ADMIN_PASSWORD=your-secure-password
export GITHUB_BACKUP_TOKEN=ghp_your_github_token
```

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=QuestionServiceUtilTest

# Run with coverage
./mvnw test jacoco:report
```

## Project Structure

```
src/main/java/com/rakesh/dsa/tracker/
├── DsaTrackerApplication.java    # Main entry point
├── config/
│   └── SecurityConfig.java       # Spring Security config
├── controller/
│   ├── ui/                       # Thymeleaf controllers
│   │   ├── HomeController.java
│   │   ├── DashboardController.java
│   │   ├── QuestionListController.java
│   │   ├── QuestionFormController.java
│   │   └── QuestionActionController.java
│   └── api/                      # REST controllers
│       ├── QuestionController.java
│       ├── SuggestionController.java
│       └── DataOpsController.java
├── service/
│   ├── QuestionService.java
│   ├── QuestionServiceImpl.java
│   ├── StatsService.java
│   ├── BackupService.java
│   └── ExcelExportService.java
├── repository/                    # JPA repositories
├── model/                        # Entities and DTOs
├── github/                       # GitHub backup logic
├── schedule/                     # Scheduled tasks
└── props/                        # Configuration properties

src/main/resources/
├── templates/                     # Thymeleaf templates
│   ├── layout/                   # Layout fragments
│   ├── fragments/                # Reusable fragments
│   └── *.html                    # Pages
├── static/css/                   # Stylesheets
├── static/js/                    # JavaScript
└── db/migration/                 # Flyway migrations
```

## GitHub Backup

The application automatically backs up your data to a GitHub repository:

1. Create a GitHub Personal Access Token with `repo` scope
2. Set the token as environment variable: `GITHUB_BACKUP_TOKEN`
3. Configure GitHub settings in your application config:
   ```yaml
   app:
     github:
       username: your-github-username
       repo: dsa-tracker-backups
       branch: main
   ```

Backups are scheduled:
- 10:00 AM daily (nightly)
- 1:00 PM daily (daily)

## Actuator Endpoints

Health and metrics available at:
- `/actuator/health` - Health check
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

## License

MIT License
