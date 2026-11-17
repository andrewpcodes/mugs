# Implementation Plan: Mug Fill Count, Tasting Log Details, and Nicknames

**Branch**: `001-mug-fills-history-nickname` | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-mug-fills-history-nickname/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Return a derived `fillCount` and an optional, owner-editable `nickname` on every mug; add an
optional `price` to tasting log entries; and add a new owner-only `POST /api/mugs/{mugId}/fills`
operation that records fills in the existing (unused) `mug_fills` table. The API contract is
extended additively first; existing SQLite databases are upgraded in place by an idempotent
startup check that adds the two new nullable columns (see [research.md](research.md)).

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.0 (Web MVC, Security OAuth2 Resource Server, JDBC,
Validation), OpenAPI Generator 7.15 (models only), MapStruct 1.6, Lombok

**Storage**: SQLite (`sqlite-jdbc`), Flyway migrations in `src/main/resources/db/migration`, foreign keys on

**Testing**: Spock 2.4 on JUnit Platform (`@WebMvcTest` for controllers; plain Spock for service
and repository)

**Target Platform**: Docker container on Raspberry Pi (ARM64) and x86_64 Linux

**Project Type**: Web service (REST API for a separate frontend)

**Performance Goals**: Mug list responses no slower than today as perceived by users (SC-005);
fill count via indexed subquery

**Constraints**: Additive, backward-compatible API and schema changes (FR-010, FR-011); no new
dependencies; low-memory single-board host

**Scale/Scope**: One mug club: hundreds of mugs, thousands of fills/history entries; 1 new
endpoint, 4 changed schemas, 2 new schemas

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Pre-design | Post-design |
|-----------|-------|------------|-------------|
| I. Contract-First API | All changes start in `api-specification.yml`; models generated; errors as `ProblemDetail` | PASS | PASS: [contracts/api-specification.delta.yml](contracts/api-specification.delta.yml) is merged first; no hand-written DTOs; additive only |
| II. Test-Backed Behavior | Success, 400, 401, 403, 404 covered by Spock; no external services | PASS | PASS: controller, service, and repository specs planned (research R8); fixed `Clock`; mocked JWTs |
| III. Secure by Default | Writes need JWT; new routes authenticated; owner check in service | PASS | PASS: `POST .../fills` matched by existing `/api/mugs/**` authenticated rule; owner check in `MugService`; existing gap fixed: `GET /api/mugs` now requires login as the contract declares (R9) |
| IV. Layered Simplicity | controller → service → repository; JDBC; no new frameworks | PASS | PASS: `Clock` bean is a small config class; Flyway added for versioned migrations, justified in R1 |
| V. Portable Deployment | Idempotent, additive schema changes; env-var config | PASS | PASS: Flyway baseline + additive V2 migration (R1); UTC everywhere, no server-side local zone (R5) |

No violations, so Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-mug-fills-history-nickname/
├── plan.md              # This file
├── research.md          # Phase 0 decisions
├── data-model.md        # Phase 1 entities and schema changes
├── quickstart.md        # Phase 1 validation guide
├── contracts/
│   └── api-specification.delta.yml   # Additions to api-specification.yml
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit-tasks; not created here)
```

### Source Code (repository root)

```text
api-specification.yml                         # + addMugFill, AddMugFillRequest, MugFillResponse,
                                              #   fields on MugResponse/UpdateMugRequest/
                                              #   AddMugHistoryRequest/MugHistoryResponse
src/main/resources/
├── application.properties                    # + spring.flyway.baseline-*
└── db/migration/V1__initial_schema.sql, V2__add_mug_nickname_and_history_price.sql
Dockerfile, docker-compose.yml, README.md     # + TZ=UTC
src/main/java/com/overmild/mugs/
├── configs/
│   ├── ClockConfig.java                      # NEW: UTC Clock
│   ├── SecurityConfig.java                   # GET /api/mugs requires login (R9)
├── controllers/
│   └── MugController.java                    # + POST /api/mugs/{mugId}/fills; pass nickname, price
├── services/
│   └── MugService.java                       # + addFill; size/date/price/nickname rules
├── repositories/
│   ├── MugRepository.java                    # + addFill; nickname on update; price on history
│   └── MugRepositoryImplementation.java      # + fill_count subquery in all mug SELECTs
├── mappers/
│   └── MugMapper.java                        # + toMugFillResponse
└── models/entities/
    ├── MugEntity.java                        # + nickname, fillCount
    ├── MugHistoryEntity.java                 # + price
    └── MugFillEntity.java                    # NEW

src/test/groovy/com/overmild/mugs/
├── controllers/MugControllerSpec.groovy      # extended
├── services/MugServiceSpec.groovy            # NEW
└── repositories/MugRepositoryImplementationSpec.groovy  # NEW (temp-file SQLite)
```

**Structure Decision**: Single Spring Boot project; keep the existing package layout and add the
new code to the existing layers (Principle IV). Generated models land in
`build/generated/openapi` under `com.overmild.mugs.models` as today.

## Implementation Order

Follows the constitution's development workflow:

1. Merge the contract delta into `api-specification.yml`; run `./gradlew openApiGenerate`.
2. Schema: Flyway V1 (existing schema) + V2 (new columns); repository spec covering upgrade.
3. Entities and repository: `nickname`, `fillCount`, `price`, `MugFillEntity`, `addFill`.
4. Service: `addFill` (owner, size, date rules), price scale, nickname trimming, with
   `MugServiceSpec`.
5. Mapper and controller: new endpoint + fields, with `MugControllerSpec` cases.
6. Run `./gradlew build`, `docker build`, then [quickstart.md](quickstart.md).

## Notes

- `createMug` currently returns 200 while the contract declares 201. This is existing behavior,
  out of scope here and left unchanged.
- Unset nickname and price are returned as `null` (research R7; spec FR-007 updated to match).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations.
