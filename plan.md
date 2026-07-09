# Backend Requirements — Gaggle of Mugs

This document outlines the API changes needed to fully support the frontend features.
The current `api-specification.yml` is minimal — several app features are broken or degraded without the additions below.

---

## 🔴 Required — App is broken without these

### 1. Return `fillCount` in `MugResponse`

The tasting log count, tier badge, milestone unlocks, and savings calculation all depend on `fillCount`. Currently it is always `0` in the frontend because the API does not return it.

**Change:** Add `fillCount: integer` to `MugResponse`.

```yaml
MugResponse:
  properties:
    fillCount:
      type: integer
      minimum: 0
      example: 12
```

---

### 2. Load tasting history on the Roost page

The frontend has a Tasting Log section on the mug detail page (`/roost/:mugId`) but `GET /api/mugs/{mugId}/history` is never called on page load — `history` always starts as `[]`. The frontend needs this endpoint to populate the log.

**Change:** No schema change needed — the endpoint already exists. The frontend will be updated to call it on mount. However, the current `MugHistoryResponse` only has `entry: string`. For a useful log the following fields are needed:

```yaml
MugHistoryResponse:
  properties:
    id:
      type: string
    mugId:
      type: string
    addedByUserId:
      type: string
    entry:
      type: string
      example: "Hazy IPA"
    price:
      type: number
      minimum: 0
      example: 6.50
    createdAt:
      type: string
      example: "2026-06-16T20:03:11.843827Z"
```

> If financial tracking (total spent, favourite fill, lifetime savings) is not in scope, `price` can be omitted and those UI panels will be hidden.

---

## 🟡 Important — UI shows blank/placeholder values without these

### 3. Return `nickname` in `MugResponse` and accept it in `UpdateMugRequest`

`nickname` is displayed prominently on the mug card and the Roost detail page. Without it the frontend falls back to `"{sizeOz}oz Mug"`. Owners can set a nickname via the Modify panel but it only saves locally — the API ignores it because `nickname` is not in `UpdateMugRequest`.

**Change:** Add `nickname` to both `MugResponse` and `UpdateMugRequest`.

```yaml
MugResponse:
  properties:
    nickname:
      type: string
      example: "The Hazy One"

UpdateMugRequest:
  properties:
    nickname:
      type: string
      example: "The Hazy One"
```

---

### 4. Return `ownerName` in `MugResponse`

Displayed as "Owned by [name]" on the Roost page for visitors. The API currently returns `ownerUserId` (UUID) but not a human-readable name. The frontend hides the row when this is empty.

**Change:** Add `ownerName: string` to `MugResponse`.

```yaml
MugResponse:
  properties:
    ownerName:
      type: string
      example: "Andrew"
```

---

### 5. Return `number` (mug club number) in `MugResponse`

Displayed as `#42` on the mug card and Roost hero banner. Without it the number is hidden from the UI entirely.

**Change:** Add `number: integer` to `MugResponse`.

```yaml
MugResponse:
  properties:
    number:
      type: integer
      minimum: 1
      example: 42
```

> If mug numbers are assigned by the club administrator rather than the user, this could also be added to `CreateMugRequest` as an optional field.

---

## 🟢 Nice to Have — Gamification features

### 6. Return or compute `tier` in `MugResponse`

The tier badge (`Gosling` → `Fledgling` → `Flyer` → `Elder` → `Golden Eagle`) is calculated from `fillCount` on the frontend. Once `fillCount` is returned (#1 above) the frontend will compute it automatically. Optionally the backend can return it pre-computed.

| Tier | Fill threshold |
|---|---|
| Gosling | 0+ |
| Fledgling | 5+ |
| Flyer | 10+ |
| Elder | 25+ |
| Golden Eagle | 50+ |

---

### 7. Persist sticker selections via `equippedStickers`

The sticker board lets owners equip earned digital badges. Currently selections only persist in local browser state and are lost on refresh.

**Change:** Add `equippedStickers` to both `MugResponse` (read) and `UpdateMugRequest` (write).

```yaml
MugResponse:
  properties:
    equippedStickers:
      type: array
      items:
        type: string
      example: ["sticker-1", "sticker-3"]

UpdateMugRequest:
  properties:
    equippedStickers:
      type: array
      items:
        type: string
```

---

## Summary

| Field / Change                    | Endpoint                                        | Priority        |
|-----------------------------------|-------------------------------------------------|-----------------|
| `fillCount`                       | `MugResponse`                                   | 🔴 Required     |
| `price` on history entries        | `MugHistoryResponse`                            | 🔴 Required     |
| `nickname` (read + write)         | `MugResponse` + `UpdateMugRequest`              | 🟡 Important    |
| `ownerName`                       | `MugResponse`                                   | 🟡 Important    |
| `number`                          | `MugResponse` (+ optionally `CreateMugRequest`) | 🟡 Important    |
| `tier` (pre-computed)             | `MugResponse`                                   | 🟢 Nice to have |
| `equippedStickers` (read + write) | `MugResponse` + `UpdateMugRequest`              | 🟢 Nice to have |
