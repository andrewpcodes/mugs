# Mugs API

A Spring Boot REST API application for managing users and locations with PostgreSQL database integration.

## Overview

Mugs is a RESTful API built with Spring Boot 4.0.1 that provides user and location management functionality. It uses PostgreSQL for data persistence, MapStruct for object mapping, and includes Spring Security configuration for local development.

## Technologies

- **Java 21** - Programming language
- **Spring Boot 4.0.1** - Application framework
- **Spring Data JPA** - Data access layer
- **Spring Security** - Security framework (configured for open access in development)
- **PostgreSQL** - Relational database
- **MapStruct 1.6.3** - Object mapping
- **Gradle** - Build tool
- **Lombok** - Code generation and boilerplate reduction
- **Hibernate** - ORM framework

## Prerequisites

- Java 21 or higher
- PostgreSQL 12 or higher
- Gradle 8.x or higher (wrapper included)

## Getting Started

### 1. Database Setup

Create a PostgreSQL database named `mugsdb`:

```sql
CREATE DATABASE mugsdb;
```

### 2. Configuration

Update `src/main/resources/application.yaml` with your database credentials:

```yaml
spring:
  datasource:
    username: your_username
    password: your_password
```

### 3. Running the Application

**Using Gradle Wrapper (recommended):**
```bash
./gradlew bootRun
```

**Using IntelliJ IDEA:**
1. Open the project in IntelliJ IDEA
2. Run the `MugsApplication` main class

The application will start on `http://localhost:8080/mugs/api`

## API Endpoints

All endpoints are prefixed with `/mugs/api`.

### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users` | Retrieve all users |
| GET | `/user/{id}` | Retrieve a specific user by ID (query param) |
| POST | `/user/create` | Create a new user |
| PATCH | `/user/update` | Update an existing user |
| DELETE | `/user/delete/{id}` | Delete a user by ID |

### Example Requests

**Get All Users:**
```bash
curl http://localhost:8080/mugs/api/users
```

**Create User:**
```bash
curl -X POST http://localhost:8080/mugs/api/user/create \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com"
  }'
```

**Get User by ID:**
```bash
curl "http://localhost:8080/mugs/api/user/{id}?id=550e8400-e29b-41d4-a716-446655440000"
```

**Update User:**
```bash
curl -X PATCH http://localhost:8080/mugs/api/user/update \
  -H "Content-Type: application/json" \
  -d '{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@example.com"
  }'
```

**Delete User:**
```bash
curl -X DELETE http://localhost:8080/mugs/api/user/delete/550e8400-e29b-41d4-a716-446655440000
```

## Project Structure

```
mugs/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/overmild/mugs/
│   │   │       ├── MugsApplication.java    # Main application class
│   │   │       ├── config/                 # Security configuration
│   │   │       ├── controller/             # REST controllers
│   │   │       ├── entity/                 # JPA entities (database)
│   │   │       ├── mapper/                 # MapStruct mappers
│   │   │       ├── model/                  # Domain models (DTOs)
│   │   │       ├── repository/             # Spring Data repositories
│   │   │       └── service/                # Business logic layer
│   │   └── resources/
│   │       ├── application.yaml            # Application configuration
│   │       ├── static/                     # Static resources
│   │       └── templates/                  # Template files
│   └── test/                               # Unit and integration tests
├── build.gradle                            # Gradle build configuration
├── gradlew                                 # Gradle wrapper (Unix)
├── gradlew.bat                             # Gradle wrapper (Windows)
└── README.md                               # This file
```

## Architecture

The application follows a **layered architecture** pattern with clear separation of concerns:

### Entity-Model Separation

- **Entities** (`entity/`): JPA entities mapped to database tables
- **Models** (`model/`): Immutable DTOs (Data Transfer Objects) exposed via API
- **Mappers** (`mapper/`): MapStruct interfaces for entity ↔ model conversion

This separation provides:
- **Decoupling**: Database schema changes don't directly impact API contracts
- **Security**: Prevents accidental exposure of sensitive entity fields
- **Flexibility**: API models can differ from database structure

### Example Flow

```
Client Request → Controller → Service → Repository → Database
                     ↓           ↓
                   Model      Entity
                   (DTO)    (Database)
```

## Security Configuration

The application is configured with **Spring Security** but allows **unauthenticated access** to all endpoints in development:

- All endpoints are publicly accessible by default
- CSRF protection is disabled (suitable for API development)
- Method-level security (`@PreAuthorize`, `@Secured`, `@RolesAllowed`) is enabled for future use

To secure specific endpoints, add annotations to controller methods:

```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<User> createUser(@RequestBody User user) {
    // Only accessible to ADMIN role
}
```

## Development

### Building the Project

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Clean Build

```bash
./gradlew clean build
```

### Code Generation

The project uses annotation processors for:
- **Lombok**: Generates getters, setters, constructors, etc.
- **MapStruct**: Generates type-safe mapper implementations
- **Spring Boot Configuration Processor**: Generates metadata for IDE autocomplete

These run automatically during compilation.

## Database Schema

The application uses **Hibernate DDL auto-update** mode (`ddl-auto: update`), which automatically creates and updates database tables based on entity definitions.

### User Entity

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | Primary Key, Auto-generated |
| first_name | VARCHAR | NOT NULL |
| last_name | VARCHAR | NOT NULL |
| email | VARCHAR | UNIQUE |
| created_at | TIMESTAMP | NOT NULL, Auto-generated |
| modified_at | TIMESTAMP | NOT NULL, Auto-updated |

## Features

- ✅ RESTful API with proper HTTP methods
- ✅ PostgreSQL integration with JPA/Hibernate
- ✅ Automatic schema management
- ✅ Entity-to-Model mapping with MapStruct
- ✅ Logging with SLF4J
- ✅ Method-level security support
- ✅ Clean architecture with separation of concerns
- ✅ UUID-based identifiers
- ✅ Automatic timestamp tracking
- ✅ Spring Boot DevTools for hot reload

## Future Enhancements

- [ ] Add authentication and authorization
- [ ] Implement pagination for user listing
- [ ] Add comprehensive input validation
- [ ] Create custom exception handling with proper error responses
- [ ] Add API documentation (Swagger/OpenAPI)
- [ ] Implement user roles and permissions
- [ ] Add integration tests
- [ ] Add database migrations (Flyway/Liquibase)
- [ ] Implement caching (Redis)
- [ ] Add API rate limiting
- [ ] Create Docker configuration
- [ ] Add health checks and metrics (Actuator)

## Troubleshooting

### Database Connection Issues

If you see "Failed to determine a suitable driver class":
1. Ensure PostgreSQL is running
2. Verify database credentials in `application.yaml`
3. Ensure the `mugsdb` database exists

### Port Already in Use

If port 8080 is already in use, change it in `application.yaml`:
```yaml
server:
  port: 8081
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Contact

For questions or support, please open an issue in the repository.

