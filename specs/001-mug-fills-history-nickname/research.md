# Research: Mug Fill Count, Tasting Log Details, and Nicknames

All Technical Context items were known from the existing codebase; the decisions below cover the
open design questions the spec leaves to planning.

## R1. Adding columns to existing SQLite tables

- **Decision** (revised after code review): Flyway (`spring-boot-starter-flyway`; SQLite support
  is in `flyway-core`). `V1__initial_schema.sql` holds the pre-feature schema and
  `V2__add_mug_nickname_and_history_price.sql` adds the two columns. Existing Pi databases have no
  history table, so `spring.flyway.baseline-on-migrate=true` with `baseline-version=1` records
  them as V1 and applies V2. `schema.sql` and `spring.sql.init.*` are removed.
- **Rationale**: Versioned, recorded migrations that can also express renames, type changes, and
  backfills, which the first approach (a startup component checking `PRAGMA table_info` before
  each `ALTER TABLE`) could not. The new dependency is justified here per Principle IV.
- **Alternatives considered**:
  - Startup check-then-`ALTER` component (`SchemaMigrations`): the original choice; no history
    and additive columns only.
  - `spring.sql.init.continue-on-error=true` with `ALTER TABLE` in `schema.sql`: hides real
    schema errors.
  - New side tables for nickname/price: avoids `ALTER`, but adds joins for no benefit.

## R2. Computing the fill count

- **Decision**: Every mug query shares one `MUG_SELECT` prefix that does
  `LEFT JOIN mug_fills f ON f.mug_id = m.id` and selects `COUNT(f.id) AS fill_count`, with
  `GROUP BY m.id`. `MugEntity` gains a `fillCount` field. `createMug` returns `fillCount = 0`.
- **Rationale**: Always correct (SC-001), no counter to keep in sync, and the existing
  `idx_mug_fills_mug_id` index makes each count an index lookup. Club-scale data (hundreds of
  mugs, thousands of fills) keeps list queries well under perceptible latency (SC-005).
- **Alternatives considered**: A stored `fill_count` column incremented on insert: faster reads
  but can drift and needs a migration backfill; rejected as premature (YAGNI).

## R3. Recording a fill

- **Decision**: New operation `POST /api/mugs/{mugId}/fills` (`addMugFill`), bearer auth,
  request `AddMugFillRequest { amountOunces (required, > 0), fillDate (optional date) }`,
  response `201 MugFillResponse`. Mirrors `POST /api/mugs/{mugId}/history`.
- **Rationale**: Follows the existing sub-resource pattern; owner-only check reuses the history
  logic (spec clarification Q1).
- **Alternatives considered**: `PATCH /api/mugs/{mugId}` incrementing a counter: loses fill date
  and amount, and the spec requires both.

## R4. Validation placement

- **Decision**:
  - In the contract (generated bean validation): `amountOunces > 0`, `price >= 0`, `price` at most 2
    decimal places (`x-field-extra-annotation` adding `@Digits(integer = 10, fraction = 2)`),
    `entry` `minLength: 1` (existing).
  - In `MugService` (needs data or rules the contract can't express), returning `400` via
    `ResponseStatusException(BAD_REQUEST)`: `amountOunces <= mug.sizeOunces`;
    `fillDate <= today` (see R5); nickname at most 50 characters after trimming (see R6).
- **Rationale**: Contract-first (Principle I) for everything expressible in OpenAPI; the
  generator does not reliably map `multipleOf` to a bean-validation constraint, and the size
  check needs the stored mug.
- **Alternatives considered**: Custom bean-validation annotations on generated models: not
  possible without editing generated code.

## R5. What "today" means

- **Decision** (per user direction: GMT everywhere, New York for local time):
  - A `java.time.Clock` bean returns `Clock.systemUTC()`; every `Instant` (`createdAt`,
    `updatedAt`) comes from it and is stored/returned in UTC.
  - A `ZoneId` bean `localZone` reads `app.time.local-zone`
    (`${APP_TIME_LOCAL_ZONE:America/New_York}` in `application.properties`).
  - A missing fill date defaults to `LocalDate.now(clock.withZone(localZone))`. A fill date is
    rejected only if it is after today in UTC+14, the furthest-ahead zone, so users east of New
    York can send their own local date (revised after code review).
  - The container runs with `TZ=UTC` (Dockerfile and Compose) so JVM defaults and logs are also
    GMT; code never relies on the JVM default zone.
- **Rationale**: One unambiguous zone for stored data; a fixed, configurable local zone for the
  one place a calendar date is needed (Principle V); fully testable with `Clock.fixed`
  (Principle II).
- **Alternatives considered**: UTC for "today" as well: a 9 pm New York fill would default to
  tomorrow's date. Host/JVM default zone: varies by machine. A time-zone request header (e.g.
  `X-Time-Zone`) to localize responses and/or "today": rejected by the user. Responses stay UTC
  so they are identical for every caller (simpler caching and tests), and the frontend localizes
  for display; it can send `fillDate` explicitly to use the user's own local day.

## R6. Nickname normalization

- **Decision** (revised after code review): `MugService.updateMug` trims the nickname, then
  rejects it with `400` if it is over 50 characters; empty or blank becomes `null` (cleared);
  absent or `null` keeps the stored nickname. The contract states the limit in the field
  description instead of `maxLength`, because `maxLength` was checked before trimming.
- **Rationale**: Clients that don't know about nicknames can't erase them with a `PUT`, and a
  padded nickname that fits after trimming is accepted.

## R7. Absent optional values in responses

- **Decision**: `nickname` (unset) and `price` (not given) are returned as JSON `null`, using the
  existing global Jackson settings.
- **Rationale**: Spec FR-007 says "omit it otherwise"; the frontend treats `null` and missing the
  same, and changing global null-inclusion would alter every existing response (FR-011). Both
  fields are declared optional and nullable in the contract.

## R8. Testing approach

(See also R9.)

- **Decision**:
  - `MugControllerSpec` (existing, `@WebMvcTest`): new endpoint and fields, plus 400/401/403/404
    paths.
  - New `MugServiceSpec` (plain Spock, mocked repository, fixed `Clock`): owner checks, size and
    date limits, price scale, nickname trimming.
  - New `MugRepositoryImplementationSpec` (plain Spock against a temp-file SQLite database with
    `schema.sql` applied, no Spring context): fill count queries, new columns, and
    `SchemaMigrations` upgrading a database created with the old schema.
- **Rationale**: Principle II requires every behavior tested without external services; the
  migration and SQL can only be proven against real SQLite.

## R9. Authentication on `GET /api/mugs`

- **Decision**: Require login. Add `.requestMatchers(HttpMethod.GET, "/api/mugs").authenticated()`
  before the `GET /api/mugs/**` permit rule in `SecurityConfig`, and test 401/200.
- **Rationale**: `api-specification.yml` already declares `bearerAuth` for this operation, but
  `/api/mugs/**` also matches `/api/mugs`, so it is public today. The contract is the source of
  truth (Principles I and III). Chosen by the user (analysis finding C1).
- **Alternatives considered**: Make it public in the contract instead: rejected by the user.
