# Copilot instructions for `mugs`

## Build and test commands

Use the Gradle wrapper from the repository root.

| Purpose | Command | Notes |
| --- | --- | --- |
| Run the application locally | `./gradlew bootRun` | The app serves under `http://localhost:8080/mugs/api/` because the context path is configured in `application.yml`. |
| Full CI-style build | `./gradlew build` | This is what `.github/workflows/build.yml` runs. |
| Full verification task | `./gradlew check` | Runs the configured verification tasks; there is no separate lint task configured in Gradle. |
| Run all tests | `./gradlew test` | Tests run on the JUnit Platform. |
| Run one Spock spec | `./gradlew test --tests 'com.overmild.mugs.controller.UserControllerSpec'` | Specs live under `src/test/groovy`. |
| Run one Spock feature | `./gradlew test --tests '*UserControllerSpec.*sample test*'` | Feature names can contain spaces, so wildcard matching is often easiest. |

## High-level architecture

This is a Spring Boot monolith rooted at `com.overmild.mugs` with a standard controller -> service -> repository flow. The HTTP layer is split across `UserController`, `MugController`, and `LocationController`; controllers use method-level mappings only, and the global `/mugs/api/` prefix comes from `server.servlet.context-path` in `src/main/resources/application.yml`.

Persistence and API contracts are deliberately separated:

- `entity/` holds JPA entities for PostgreSQL tables (`UserEntity`, `MugEntity`, `LocationEntity`).
- `model/` holds API-facing models (`User`, `Mug`, `Location`) that controllers and services expose.
- `mapper/` contains MapStruct mappers that convert between entities and models as Spring beans.

The core domain relationship is `MugEntity` as the join point between users and locations: each mug belongs to one user and one location, while users and locations each expose `Set<MugEntity>` collections. Repository methods add fetch-join queries where nested graph loading matters, especially around mugs and their related locations/users.

Security is present but effectively opened for development: `SecurityConfig` permits all requests and disables CSRF. If a change introduces real authorization behavior, it needs explicit security updates rather than assuming endpoints are already protected.

## Key conventions

- Keep API models and persistence entities separate. New controller/service work should usually operate on `model/*`, not return JPA entities directly.
- `model/*` classes are primarily immutable Lombok `@Value` types, while `entity/*` classes use mutable Lombok/JPA patterns such as `@Data`.
- MapStruct mappers are `componentModel = "spring"`. When mapping model -> entity, persistence-managed fields and back-reference collections are intentionally ignored rather than copied from requests.
- Services own logging, mapping, and repository interaction. They commonly return `null` for not-found reads or invalid updates instead of throwing custom exceptions.
- Transaction boundaries are placed in the service layer with `@Transactional`, especially around reads that touch lazy relationships and around write operations.
- Tests are written as Spock specs in `src/test/groovy`, even though Gradle runs them through the JUnit Platform. When adding tests, match that setup instead of defaulting to JUnit test classes in `src/test/java`.
