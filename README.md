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

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/register` | Register a new USER-role account | No |

### User Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/users` | Retrieve all users | No |
| GET | `/users/{id}` | Retrieve a specific user by ID | No |
| GET | `/users/{userId}/mugs` | Retrieve all mugs for a specific user | No |
| POST | `/users` | Create a new user | ADMIN |
| PUT | `/users` | Update an existing user | ADMIN |
| DELETE | `/users/{id}` | Delete a user by ID | ADMIN |

### Mug Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/mugs` | Retrieve all mugs | No |
| GET | `/mugs/{id}` | Retrieve a specific mug by ID | No |
| POST | `/mugs` | Create a new mug | ADMIN |
| PUT | `/mugs` | Update an existing mug | ADMIN |
| DELETE | `/mugs/{id}` | Delete a mug by ID | ADMIN |

### Location Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/locations` | Retrieve all locations | No |
| GET | `/locations/{id}` | Retrieve a specific location by ID | No |
| POST | `/locations` | Create a new location | ADMIN |
| PUT | `/locations` | Update an existing location | ADMIN |
| DELETE | `/locations/{id}` | Delete a location by ID | ADMIN |

### Example Requests

**Get All Users:**
```bash
curl http://localhost:8080/users
```

**Register an application user:**
```bash
curl -X POST http://localhost:8080/mugs/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"s3cr3t"}'
```

**Get All Users (public):**
```bash
curl http://localhost:8080/mugs/api/users
```

**Get User by ID (public):**
```bash
curl http://localhost:8080/mugs/api/users/550e8400-e29b-41d4-a716-446655440000
```

**Create User (ADMIN required):**
```bash
curl -X POST http://localhost:8080/mugs/api/users \
  -u admin:adminpassword \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com"
  }'
```

**Update User (ADMIN required):**
```bash
curl -X PUT http://localhost:8080/mugs/api/users \
  -u admin:adminpassword \
  -H "Content-Type: application/json" \
  -d '{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@example.com"
  }'
```

**Delete User (ADMIN required):**
```bash
curl -X DELETE http://localhost:8080/mugs/api/users/550e8400-e29b-41d4-a716-446655440000 \
  -u admin:adminpassword
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

The application uses **HTTP Basic Authentication** backed by a dedicated `app_user` database table.
Passwords are stored as **BCrypt** hashes. Sessions are stateless; credentials must be supplied on every protected request.

### Roles

| Role | Permissions |
|------|-------------|
| `USER` | Read-only access to public GET endpoints |
| `ADMIN` | Full access — can create, update, and delete all resources |

### Authorization rules

| Method | Endpoint pattern | Required role |
|--------|-----------------|---------------|
| GET | `/**` | None (public) |
| POST | `/auth/register` | None (public) |
| POST, PUT, DELETE | `/**` | `ADMIN` |

### Authentication flow

1. **Register** a new account (receives the `USER` role):
   ```bash
   curl -X POST http://localhost:8080/mugs/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"alice","password":"s3cr3t"}'
   ```

2. **Call a protected endpoint** by supplying HTTP Basic credentials:
   ```bash
   curl -X POST http://localhost:8080/mugs/api/users \
     -u admin:adminpassword \
     -H "Content-Type: application/json" \
     -d '{"firstName":"John","lastName":"Doe","email":"john@example.com"}'
   ```
   Unauthenticated requests to protected endpoints receive **401 Unauthorized**.
   Authenticated requests with an insufficient role receive **403 Forbidden**.

3. **Bootstrap an ADMIN account** by inserting a row directly into the `app_user` table
   (role = `ADMIN`, password must be a BCrypt hash):
   ```sql
   INSERT INTO app_user (id, username, password, role)
   VALUES (gen_random_uuid(),
           'admin',
           '$2a$12$<bcrypt-hash-of-your-password>',
           'ADMIN');
   ```
   Use a tool such as [bcrypt-generator.com](https://bcrypt-generator.com/) or `htpasswd -bnBC 12 "" yourpassword | tr -d ':\n'` to generate the hash.

### Additional security notes

- CSRF protection is disabled (stateless REST API).
- Use HTTPS in production to protect Basic Auth credentials in transit.

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

- [x] Add authentication and authorization
- [ ] Implement pagination for user listing
- [ ] Add comprehensive input validation
- [ ] Create custom exception handling with proper error responses
- [ ] Add API documentation (Swagger/OpenAPI)
- [x] Implement user roles and permissions
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

