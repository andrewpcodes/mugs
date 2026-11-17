# Data Model: Mug Fill Count, Tasting Log Details, and Nicknames

Changes are additive. Existing columns, tables, and relationships are unchanged.

## Mug (`mugs`)

| Field | Column | Type | Change | Rules |
|-------|--------|------|--------|-------|
| nickname | `nickname` | TEXT, nullable | **New** | Trimmed; 1–50 chars; blank or absent on update → `NULL` (cleared). Owner-only (existing update rule). |
| fillCount | — (derived) | integer | **New, read-only** | `COUNT(*)` of `mug_fills` rows for the mug; 0 when none. |

`MugEntity` gains `nickname` and `fillCount`. Existing mugs get `nickname = NULL` on upgrade.

## Fill (`mug_fills`, table already exists, first use)

| Field | Column | Type | Rules |
|-------|--------|------|-------|
| id | `id` | TEXT PK | UUID, server-generated |
| mugId | `mug_id` | TEXT FK → `mugs.id` | Mug must exist (404 otherwise); `ON DELETE CASCADE` (existing) |
| filledByUserId | `filled_by_user_id` | TEXT | JWT subject; must equal mug owner (403 otherwise) |
| fillDate | `fill_date` | TEXT (ISO date) | Required on input (the user's local date); must not be after today in UTC+14 |
| amountOunces | `amount_ounces` | NUMERIC | > 0 and ≤ the mug's `size_ounces` |
| createdAt | `created_at` | TEXT (ISO instant, UTC) | Server-generated |

New record `MugFillEntity` with these fields. No price (spec clarification Q2).

## Tasting Log Entry (`mug_history`)

| Field | Column | Type | Change | Rules |
|-------|--------|------|--------|-------|
| price | `price` | NUMERIC, nullable | **New** | Optional; ≥ 0; at most 2 decimal places. Existing rows → `NULL`. |

`MugHistoryEntity` gains `price`. All other fields (id, mugId, addedByUserId, entry, createdAt)
are already stored and returned. Order: `created_at DESC` (existing).

## Relationships

```text
mug_owners 1 ──< mugs 1 ──< mug_history
                       └──< mug_fills
```

## Schema upgrade (existing databases)

Flyway migrations in `src/main/resources/db/migration`:

- `V1__initial_schema.sql`: the schema before this feature.
- `V2__add_mug_nickname_and_history_price.sql`:
  - `ALTER TABLE mugs ADD COLUMN nickname TEXT`
  - `ALTER TABLE mug_history ADD COLUMN price NUMERIC`

Databases created before Flyway are baselined at V1 (`spring.flyway.baseline-on-migrate`), then
V2 runs. Fresh databases run V1 and V2. No data is rewritten.
