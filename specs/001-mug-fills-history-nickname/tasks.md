---

description: "Task list for Mug Fill Count, Tasting Log Details, and Nicknames"
---

# Tasks: Mug Fill Count, Tasting Log Details, and Nicknames

**Input**: Design documents from `/specs/001-mug-fills-history-nickname/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-specification.delta.yml, quickstart.md

**Tests**: Included. Constitution Principle II requires Spock specs for every endpoint behavior (success, 400, 401, 403, 404), and plan.md / research R8 names the spec files.

**Organization**: Tasks are grouped by user story (spec.md US1–US4) so each can be implemented and tested independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Paths are relative to the repository root. Java base: `src/main/java/com/overmild/mugs/`; Groovy tests base: `src/test/groovy/com/overmild/mugs/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Contract first (Principle I) and the feature branch

- [X] T001 Create and switch to git branch `001-mug-fills-history-nickname` from the current branch (`git checkout -b 001-mug-fills-history-nickname`)
- [X] T002 Merge every change from `specs/001-mug-fills-history-nickname/contracts/api-specification.delta.yml` into `api-specification.yml`: add path `/api/mugs/{mugId}/fills` (POST `addMugFill`, bearerAuth, responses 201/400/401/403/404); add schemas `AddMugFillRequest` (required `amountOunces`: number, `exclusiveMinimum: true`, `minimum: 0`; optional `fillDate`: string `format: date`) and `MugFillResponse` (required `id`, `mugId`, `filledByUserId`, `fillDate` (date), `amountOunces` (number), `createdAt` (string)); add to `UpdateMugRequest` optional `nickname` (string, `maxLength: 50`, `nullable: true`); add to `AddMugHistoryRequest` optional `price` (number, `minimum: 0`, `nullable: true`); add to `MugResponse` `fillCount` (integer, `minimum: 0`, appended to `required`) and optional `nickname` (string, `nullable: true`); add to `MugHistoryResponse` optional `price` (number, `minimum: 0`, `nullable: true`). Do not change any existing field or required list other than appending `fillCount` to `MugResponse.required`
- [X] T003 Run `./gradlew openApiGenerate` and confirm `build/generated/openapi/src/main/java/com/overmild/mugs/models/` contains `AddMugFillRequest.java` and `MugFillResponse.java`, and that `MugResponse`, `MugHistoryResponse`, `UpdateMugRequest`, `AddMugHistoryRequest` have the new getters

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema columns, safe upgrade of existing databases, entity fields, and the clock: all shared by several stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 In `src/main/resources/schema.sql` add column `nickname TEXT` (nullable) to the `CREATE TABLE IF NOT EXISTS mugs` statement (after `size_ounces`) and column `price NUMERIC` (nullable) to the `CREATE TABLE IF NOT EXISTS mug_history` statement (after `entry`). Leave `mug_fills` and all indexes unchanged
- [X] T005 [P] Create `src/main/java/com/overmild/mugs/configs/SchemaMigrations.java`: a `@Component` annotated `@DependsOnDatabaseInitialization` with a `@PostConstruct` method that, using `JdbcTemplate`, reads `PRAGMA table_info(mugs)` / `PRAGMA table_info(mug_history)` and runs `ALTER TABLE mugs ADD COLUMN nickname TEXT` and `ALTER TABLE mug_history ADD COLUMN price NUMERIC` only when that column is missing; log at info level when a column is added. Must be safe to run on every start (research R1)
- [X] T006 [P] Create `src/main/java/com/overmild/mugs/configs/ClockConfig.java`: a `@Configuration` exposing `@Bean Clock clock()` returning `Clock.systemUTC()` and `@Bean ZoneId localZone(@Value("${app.time.local-zone}") String zone)` returning `ZoneId.of(zone)`; add `app.time.local-zone=${APP_TIME_LOCAL_ZONE:America/New_York}` to `src/main/resources/application.properties` (research R5: GMT/UTC for all times, New York for local dates)
- [X] T007 Add fields to entity records: `src/main/java/com/overmild/mugs/models/entities/MugEntity.java` append `String nickname, int fillCount` after `updatedAt`; `src/main/java/com/overmild/mugs/models/entities/MugHistoryEntity.java` append `BigDecimal price` after `createdAt`. Update every constructor call so the project compiles without behavior change yet: in `src/main/java/com/overmild/mugs/repositories/MugRepositoryImplementation.java` `mugRowMapper()` pass `null, 0` and `historyRowMapper()` pass `null`; in `src/main/java/com/overmild/mugs/services/MugService.java` `createMug` pass `null, 0` and `addHistoryEntry` pass `null`; in `src/test/groovy/com/overmild/mugs/controllers/MugControllerSpec.groovy` update every `new MugEntity(...)` / `new MugHistoryEntity(...)`
- [X] T007a In `src/main/java/com/overmild/mugs/configs/SecurityConfig.java` add `.requestMatchers(HttpMethod.GET, "/api/mugs").authenticated()` immediately before the `.requestMatchers(HttpMethod.GET, "/api/mugs/**").permitAll()` rule, so the all-mugs list requires login as `api-specification.yml` declares (constitution III, research R9)
- [X] T007b [P] In `src/test/groovy/com/overmild/mugs/controllers/MugControllerSpec.groovy` add tests: `GET /api/mugs` without a token → 401 and no service call; with `jwt()` → 200 and `mugService.getAllMugs()` called once
- [X] T008 Create `src/test/groovy/com/overmild/mugs/repositories/MugRepositoryImplementationSpec.groovy`: plain Spock spec (no Spring context) that in `setup()` creates a temp-file SQLite `DriverManagerDataSource` (`jdbc:sqlite:<tempfile>`, with `PRAGMA foreign_keys=ON`), applies `classpath:schema.sql` with `ResourceDatabasePopulator`, and builds `MugRepositoryImplementation` over a `NamedParameterJdbcTemplate`; include a helper to insert a `mug_owners` row and a mug. Add tests for `SchemaMigrations`: (a) on a database created with the OLD schema (table definitions without `nickname`/`price`, inlined in the spec), running it adds both columns and keeps existing rows; (b) running it twice does not fail
- [X] T009 Run `./gradlew test` and confirm all existing and new specs pass

**Checkpoint**: Schema, upgrade path, and entity shapes ready. User stories can start.

---

## Phase 3: User Story 1 - See how many times a mug has been filled (Priority: P1) 🎯 MVP

**Goal**: Every mug response includes `fillCount` = number of `mug_fills` rows for that mug (FR-001, FR-002); tasting log entries do not count.

**Independent Test**: Insert fill rows directly in the repository spec (or via US2 once done), then fetch the mug single, in `GET /api/mugs`, and in `GET /api/mugs/owners/{ownerId}`; counts match; a new mug returns `fillCount: 0`.

### Tests for User Story 1

- [X] T010 [P] [US1] In `src/test/groovy/com/overmild/mugs/repositories/MugRepositoryImplementationSpec.groovy` add tests: `findById`, `getAllMugs`, `findByOwnerUserId`, and `updateMugByOwner` return `fillCount` equal to the number of `mug_fills` rows inserted for each mug (use two mugs with different counts, e.g. 0 and 3); `mug_history` rows do not change `fillCount`
- [X] T011 [P] [US1] In `src/test/groovy/com/overmild/mugs/controllers/MugControllerSpec.groovy` add assertions that `GET /api/mugs/{mugId}`, `GET /api/mugs`, and `GET /api/mugs/owners/{ownerId}` responses contain `$.fillCount` / `$[0].fillCount` from the mapped `MugResponse`

### Implementation for User Story 1

- [X] T012 [US1] In `src/main/java/com/overmild/mugs/repositories/MugRepositoryImplementation.java` change the SELECTs in `getAllMugs`, `findById`, and `findByOwnerUserId` to read from `mugs m` and add `(SELECT COUNT(*) FROM mug_fills f WHERE f.mug_id = m.id) AS fill_count`; in `mugRowMapper()` read `resultSet.getInt("fill_count")` instead of `0` (research R2)
- [X] T013 [US1] Create `src/test/groovy/com/overmild/mugs/services/MugServiceSpec.groovy` (plain Spock, `MugRepository` mocked, `Clock.fixed(Instant.parse("2026-11-17T03:00:00Z"), ZoneOffset.UTC)` and `localZone = ZoneId.of("America/New_York")`) with a test that `createMug` returns an entity with `fillCount == 0` and `nickname == null` and calls `mugRepository.createMug` once; confirm `src/main/java/com/overmild/mugs/mappers/MugMapper.java` maps `MugEntity.fillCount` to `MugResponse.fillCount` (MapStruct maps by name; fix with `@Mapping` if the build warns of an unmapped target)

**Checkpoint**: US1 complete. Fill counts are correct for any fills present in the database.

---

## Phase 4: User Story 2 - Record a fill on a mug (Priority: P1)

**Goal**: Owner-only `POST /api/mugs/{mugId}/fills` records a fill with `amountOunces` (required, > 0, ≤ mug's `sizeOunces`) and `fillDate` (optional, defaults to today, not in the future) (FR-002a–d).

**Independent Test**: As the owner, POST a fill and get 201 with all `MugFillResponse` fields; bad amount/date → 400; other user → 403; no token → 401; unknown mug → 404.

### Tests for User Story 2

- [X] T014 [P] [US2] In `src/test/groovy/com/overmild/mugs/repositories/MugRepositoryImplementationSpec.groovy` add a test that `addFill` inserts a `mug_fills` row with all fields and that `findById` then returns `fillCount` increased by 1
- [X] T015 [P] [US2] Add to `src/test/groovy/com/overmild/mugs/services/MugServiceSpec.groovy` (created in T013; clock fixed at `2026-11-17T03:00:00Z`, which is 2026-11-16 22:00 in New York) `addFill` tests: owner with amount 12 on a 12 oz mug and no date → saves fill with `fillDate` 2026-11-16 (New York date, not the UTC date) and `createdAt` `2026-11-17T03:00:00Z`, `filledByUserId` = requester; explicit past date kept; amount 12.5 on a 12 oz mug → 400; `fillDate` 2026-11-17 → 400 (future in New York); non-owner → 403; missing mug → 404; on every rejection `addFill` is never called on the repository
- [X] T016 [P] [US2] In `src/test/groovy/com/overmild/mugs/controllers/MugControllerSpec.groovy` add tests for `POST /api/mugs/{mugId}/fills`: with `jwt()` and body `{"amountOunces":12}` → 201 and JSON `id`, `mugId`, `filledByUserId`, `fillDate`, `amountOunces`, `createdAt`; `{"amountOunces":0}` → 400 with no service call; missing `amountOunces` → 400; no token → 401; service throwing `ResponseStatusException` 403 / 404 / 400 → same status as `application/problem+json`

### Implementation for User Story 2

- [X] T017 [P] [US2] Create `src/main/java/com/overmild/mugs/models/entities/MugFillEntity.java`: `public record MugFillEntity(String id, String mugId, String filledByUserId, LocalDate fillDate, BigDecimal amountOunces, Instant createdAt)`
- [X] T018 [US2] Add `void addFill(MugFillEntity fill)` to `src/main/java/com/overmild/mugs/repositories/MugRepository.java` and implement it in `src/main/java/com/overmild/mugs/repositories/MugRepositoryImplementation.java` as `INSERT INTO mug_fills (id, mug_id, filled_by_user_id, fill_date, amount_ounces, created_at)` storing dates/instants with `toString()` like `createMug`
- [X] T019 [US2] Add `MugFillEntity addFill(String mugId, String requesterUserId, BigDecimal amountOunces, LocalDate fillDate)` to `src/main/java/com/overmild/mugs/services/MugService.java` (`@Transactional`; inject `Clock clock` and `ZoneId localZone`; `today = LocalDate.now(clock.withZone(localZone))`): load mug or throw 404 "Mug not found"; if requester is not `ownerUserId` throw 403 "Only the mug creator can add fills"; if `amountOunces.compareTo(mug.sizeOunces()) > 0` throw 400 "amountOunces must not exceed the mug's sizeOunces"; default `fillDate` to `today`; if `fillDate.isAfter(today)` throw 400 "fillDate must not be in the future"; build entity with `UUID.randomUUID()` and `Instant.now(clock)`, save, return it
- [X] T020 [US2] Add `MugFillResponse toMugFillResponse(MugFillEntity fill)` to `src/main/java/com/overmild/mugs/mappers/MugMapper.java`
- [X] T021 [US2] Add to `src/main/java/com/overmild/mugs/controllers/MugController.java` `@PostMapping("/api/mugs/{mugId}/fills") @ResponseStatus(HttpStatus.CREATED) public MugFillResponse addFill(@AuthenticationPrincipal Jwt jwt, @PathVariable @NotBlank String mugId, @Valid @RequestBody AddMugFillRequest request)` calling `mugService.addFill(mugId, jwt.getSubject(), request.getAmountOunces(), request.getFillDate())`, with an info log like the other handlers. No `SecurityConfig` change needed (the existing `/api/mugs/**` authenticated rule covers POST)

**Checkpoint**: US1 + US2 together give working, user-recordable fill counts.

---

## Phase 5: User Story 3 - View a detailed tasting log with prices (Priority: P1)

**Goal**: Owner can send an optional `price` (≥ 0, at most 2 decimal places) when adding a tasting log entry; history returns `price` (null when absent) with all other fields, newest first (FR-003–FR-006).

**Independent Test**: Add entries with price 6.50 and without price; `GET /api/mugs/{mugId}/history` returns both with correct `price`; price -1 or 6.505 → 400.

### Tests for User Story 3

- [X] T022 [P] [US3] In `src/test/groovy/com/overmild/mugs/repositories/MugRepositoryImplementationSpec.groovy` add tests: `addHistoryEntry` with price `6.50` and with `null` round-trip through `findHistoryByMugId`; a row inserted without the `price` column reads back with `price == null`; results are ordered newest first
- [X] T023 [P] [US3] In `src/test/groovy/com/overmild/mugs/services/MugServiceSpec.groovy` add `addHistoryEntry` tests: price `6.50` saved; `null` price saved as null; price `6.505` → 400 "price must have at most 2 decimal places" and repository not called; existing owner/404 checks still hold
- [X] T024 [P] [US3] In `src/test/groovy/com/overmild/mugs/controllers/MugControllerSpec.groovy` add tests: `POST /api/mugs/{mugId}/history` with `{"entry":"Hazy IPA","price":6.50}` → 201 and `$.price` 6.5 and service called with price; `{"entry":"Stout"}` → 201 and service called with `null`; `{"entry":"x","price":-1}` → 400 with no service call; `GET /api/mugs/{mugId}/history` returns `id`, `mugId`, `addedByUserId`, `entry`, `price`, `createdAt`

### Implementation for User Story 3

- [X] T025 [US3] In `src/main/java/com/overmild/mugs/repositories/MugRepositoryImplementation.java` add `price` to the `findHistoryByMugId` SELECT and the `addHistoryEntry` INSERT; in `historyRowMapper()` read `resultSet.getBigDecimal("price")` (null when SQL NULL) instead of `null`
- [X] T026 [US3] In `src/main/java/com/overmild/mugs/services/MugService.java` change `addHistoryEntry` to `addHistoryEntry(String mugId, String requesterUserId, String entry, BigDecimal price)`; after the existing 404/403 checks, if `price != null && price.stripTrailingZeros().scale() > 2` throw 400 "price must have at most 2 decimal places"; pass `price` into `MugHistoryEntity`
- [X] T027 [US3] In `src/main/java/com/overmild/mugs/controllers/MugController.java` `addHistory` pass `request.getPrice()` to `mugService.addHistoryEntry`; confirm `MugMapper.toMugHistoryResponse` maps `price` by name

**Checkpoint**: Tasting log shows prices; old entries return `price: null`.

---

## Phase 6: User Story 4 - Give a mug a nickname (Priority: P2)

**Goal**: Owner sets/clears `nickname` via `PUT /api/mugs/{mugId}`; trimmed, 1–50 chars; absent, empty, or blank clears it; every mug response includes `nickname` (null when unset) (FR-007–FR-010).

**Independent Test**: PUT with `"  The Hazy One "` → `nickname: "The Hazy One"`; PUT without nickname → `nickname: null`; 51 chars → 400; other user → 403.

### Tests for User Story 4

- [X] T028 [P] [US4] In `src/test/groovy/com/overmild/mugs/repositories/MugRepositoryImplementationSpec.groovy` add tests: `updateMugByOwner` with nickname `"The Hazy One"` persists it and `findById`/`getAllMugs`/`findByOwnerUserId` return it; update with `null` clears it; a mug row without a nickname reads back `nickname == null`
- [X] T029 [P] [US4] In `src/test/groovy/com/overmild/mugs/services/MugServiceSpec.groovy` add `updateMug` tests: nickname `"  The Hazy One "` passed to repository as `"The Hazy One"`; `null`, `""`, and `"   "` passed as `null`; existing 403/404/409 behavior unchanged
- [X] T030 [P] [US4] In `src/test/groovy/com/overmild/mugs/controllers/MugControllerSpec.groovy` add tests: `PUT /api/mugs/{mugId}` with `"nickname":"The Hazy One"` → 200 with `$.nickname` and service called with the nickname; without `nickname` → service called with `null`; nickname of 51 characters → 400 with no service call; `GET /api/mugs/{mugId}` includes `$.nickname`

### Implementation for User Story 4

- [X] T031 [US4] In `src/main/java/com/overmild/mugs/repositories/MugRepository.java` and `MugRepositoryImplementation.java` add a `String nickname` parameter to `updateMugByOwner` and set `nickname = :nickname` in the UPDATE; add `m.nickname` to the SELECTs in `getAllMugs`, `findById`, `findByOwnerUserId`; in `mugRowMapper()` read `resultSet.getString("nickname")` instead of `null`
- [X] T032 [US4] In `src/main/java/com/overmild/mugs/services/MugService.java` add a `String nickname` parameter to `updateMug`; normalize with `nickname == null || nickname.isBlank() ? null : nickname.strip()` and pass to `updateMugByOwner` (research R6)
- [X] T033 [US4] In `src/main/java/com/overmild/mugs/controllers/MugController.java` `updateMug` pass `request.getNickname()` to `mugService.updateMug`; confirm `MugMapper.toMugResponse` maps `nickname` by name

- [X] T033a [P] [US2] In `src/test/groovy/com/overmild/mugs/repositories/MugRepositoryImplementationSpec.groovy` add a test: a mug with 2 `mug_fills` rows and 2 `mug_history` rows; after `deleteMugByOwner`, both tables have 0 rows for that mug id (relies on `PRAGMA foreign_keys=ON` set in T008)

**Checkpoint**: All four stories work independently and together.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T034 Run `./gradlew build` and fix any MapStruct unmapped-target warnings or test failures
- [ ] T035 Run `docker build -t mugs:dev .` and confirm the image builds
- [ ] T036 Execute `specs/001-mug-fills-history-nickname/quickstart.md` sections 2 (upgrade a copy of `mugs.db`, start twice) and 3 (scenarios 1–18) against a local run
- [X] T037 [P] Update `plan.md` at the repository root (frontend backend-requirements doc): mark items 1–3 as done and note the new `POST /api/mugs/{mugId}/fills` endpoint the frontend must call to record fills, plus the `price` field on `AddMugHistoryRequest`; note that all API timestamps are UTC and the frontend must convert them to local time for display, and that it should send `fillDate` (the user's local date) when recording a fill, since the server's default "today" is New York time
- [X] T038 [P] Set GMT for the runtime and document the local zone: add `ENV TZ=UTC` to the runtime stage of `Dockerfile`; add `TZ: UTC` and `APP_TIME_LOCAL_ZONE: America/New_York` to the service `environment` in `docker-compose.yml`; add `-e TZ=UTC -e APP_TIME_LOCAL_ZONE=America/New_York` to the `docker run` example in `README.md` with a one-line note that all times are UTC and fill dates use the local zone

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 → T002 → T003
- **Foundational (Phase 2)**: depends on Setup. T004 before T008; T005, T006 parallel; T007 after T003 (generated models must compile); T007a/T007b after T007; T009 last
- **User Stories (Phases 3–6)**: all depend on Foundational
- **Polish (Phase 7)**: depends on all stories

### User Story Dependencies

- **US1 (P1)**: independent; testable with fills inserted directly in the repository spec
- **US2 (P1)**: independent (T033a sits in Phase 6 only to follow the shared-file order); uses US1's count only in its repository test assertion (T014). If US1 isn't done, assert the row directly instead
- **US3 (P1)**: independent
- **US4 (P2)**: independent

### Shared-file caution

US1–US4 all edit `MugRepositoryImplementation.java`, `MugService.java`, `MugController.java`, and the three spec files. Stories are logically independent, but running them truly in parallel will cause edit conflicts in those files. Run stories sequentially (US1 → US2 → US3 → US4); use [P] only within a story.

### Within Each Story

- Tests first (write, see them fail), then entity → repository → service → mapper/controller

### Parallel Opportunities

- Phase 2: T005 and T006
- Each story's three test tasks are in different files and can be written in parallel
- T017 (new entity file) in parallel with US2 tests
- T037 and T038 in parallel with T034–T036

---

## Parallel Example: User Story 2

```bash
# Tests together (different files):
Task: "T014 addFill repository test in src/test/groovy/com/overmild/mugs/repositories/MugRepositoryImplementationSpec.groovy"
Task: "T015 addFill service tests in src/test/groovy/com/overmild/mugs/services/MugServiceSpec.groovy"
Task: "T016 POST /fills controller tests in src/test/groovy/com/overmild/mugs/controllers/MugControllerSpec.groovy"

# New entity alongside tests:
Task: "T017 MugFillEntity in src/main/java/com/overmild/mugs/models/entities/MugFillEntity.java"
```

---

## Implementation Strategy

### MVP First

1. Phases 1–2 (contract, schema, upgrade path, entities)
2. Phase 3 (US1, fill counts) + Phase 4 (US2, recording fills). Fill counts are only useful once fills can be recorded, so the practical MVP is US1 + US2
3. **STOP and VALIDATE** with quickstart scenarios 1–9 and 17

### Incremental Delivery

1. Setup + Foundational → deployable (no visible change; existing DBs upgraded)
2. + US1 + US2 → fill counts and recording (frontend "Required" #1)
3. + US3 → tasting log prices (frontend "Required" #2)
4. + US4 → nicknames (frontend "Important" #3)

---

## Notes

- Commit after each task or logical group
- Every 400/403/404 from the service uses `ResponseStatusException`, which `ApiExceptionHandler` already turns into `application/problem+json`
- `createMug` returning 200 instead of the contract's 201 is existing behavior and out of scope (plan.md Notes)
