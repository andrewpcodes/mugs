# Feature Specification: Mug Fill Count, Tasting Log Details, and Nicknames

**Feature Branch**: `001-mug-fills-history-nickname`

**Created**: 2026-11-16

**Status**: Draft

**Input**: User description: "Add fillCount to MugResponse, expand MugHistoryResponse (id, mugId, addedByUserId, entry, price, createdAt), and support nickname on mugs, per plan.md"

## Clarifications

### Session 2026-11-16

- Q: Who should be allowed to record a fill on a mug? → A: Only the mug's owner (no staff/bartender role).
- Q: Should the price paid be recorded on the fill or on the tasting log entry? → A: On the tasting log entry only; fills have no price.
- Q: Should the API reject a fill that's larger than the mug's size? → A: Yes; amount must be > 0 and ≤ the mug's size.
- Q: Which time zone does the app use? → A: GMT/UTC for all stored and returned times; New York time (America/New_York) wherever a local date is needed (a fill's "today").
- Q: Should the API localize timestamps using a time-zone request header? → A: No; the API always returns UTC and the frontend converts to the user's local time.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See how many times a mug has been filled (Priority: P1)

A mug club member (or a visitor viewing a mug) opens a mug's card or detail page and sees how
many times that mug has been filled. The frontend uses this number for the tasting log count,
tier badge, milestone unlocks, and savings calculation, all of which currently show zero.

**Why this priority**: Marked "Required" in `plan.md`; several frontend features are broken
without it.

**Independent Test**: Record fills for a mug (see User Story 2), then fetch that mug
(individually, in the all-mugs list, and in the owner's mug list) and confirm the fill count
matches.

**Acceptance Scenarios**:

1. **Given** a mug with 12 recorded fills, **When** anyone fetches that mug, **Then** the
   response includes a fill count of 12.
2. **Given** a newly created mug with no fills, **When** it is fetched or returned from
   creation, **Then** the fill count is 0 (never missing).
3. **Given** a mug list containing mugs with different numbers of fills, **When** the list is
   fetched, **Then** each mug shows its own correct fill count.
4. **Given** a mug with tasting log entries but no recorded fills, **When** it is fetched,
   **Then** the fill count is 0 (log entries are not fills).

---

### User Story 2 - Record a fill on a mug (Priority: P1)

When a mug owner's mug is filled, the owner records the fill: how many ounces were poured and
on what date. Each recorded fill increases that mug's fill count by one.

**Why this priority**: Fills are tracked separately from the tasting log, so the fill count in
User Story 1 stays at 0 until fills can be recorded.

**Independent Test**: As the owner, record a fill on a mug and confirm the mug's fill count
goes up by one; confirm invalid and unauthorized attempts are rejected and do not change it.

**Acceptance Scenarios**:

1. **Given** an owner's mug with fill count 4, **When** the owner records a 12 oz fill dated
   today, **Then** the fill is saved, the response returns the new fill (id, mug id, who filled
   it, date, ounces, created time), and the mug's fill count becomes 5.
2. **Given** the owner records a fill without a date, **When** it is saved, **Then** the fill
   date defaults to today.
3. **Given** a mug owned by someone else, **When** a different member tries to record a fill,
   **Then** the request is forbidden and the fill count is unchanged.
4. **Given** no login, **When** anyone tries to record a fill, **Then** the request is rejected
   as unauthorized.
5. **Given** the owner submits 0 or negative ounces, more ounces than the mug's size, or a
   fill date in the future, **When** it is submitted, **Then** it is rejected as an invalid
   request and nothing is saved.
6. **Given** a mug that does not exist, **When** someone tries to record a fill on it,
   **Then** the request returns "not found".

---

### User Story 3 - View a detailed tasting log with prices (Priority: P1)

A member opens a mug's detail page and sees its tasting log: each entry shows what was
poured, the price paid, who added it, and when. The mug owner can record the price when adding
an entry, so the frontend can show total spent, favourite fill, and lifetime savings.

**Why this priority**: Marked "Required" in `plan.md`; the tasting log and financial panels
are empty or hidden without it.

**Independent Test**: As the owner, add log entries with and without a price, then fetch the
mug's history and confirm every field is returned correctly.

**Acceptance Scenarios**:

1. **Given** the owner adds an entry "Hazy IPA" with price 6.50, **When** the history is
   fetched, **Then** that entry shows id, mug id, the owner as the person who added it, the
   entry text "Hazy IPA", price 6.50, and the time it was created.
2. **Given** the owner adds an entry without a price, **When** the history is fetched,
   **Then** the entry is returned with no price, and all other fields present.
3. **Given** entries that existed before this feature, **When** the history is fetched,
   **Then** they are returned with no price and all other fields intact.
4. **Given** the owner submits an entry with a negative price, **When** it is submitted,
   **Then** it is rejected as an invalid request and nothing is saved.
5. **Given** a mug with several entries, **When** the history is fetched, **Then** entries
   are returned newest first.

---

### User Story 4 - Give a mug a nickname (Priority: P2)

A mug owner sets a nickname (e.g., "The Hazy One") from the Modify panel. The nickname is saved
and shown on the mug card and detail page for everyone, instead of the "{size}oz Mug"
fallback.

**Why this priority**: Marked "Important" in `plan.md`; the app works without it but shows
placeholder text and silently loses the owner's edit.

**Independent Test**: As the owner, update a mug with a nickname, then fetch it and confirm
the nickname is returned; clear it and confirm it is gone.

**Acceptance Scenarios**:

1. **Given** an owner's mug without a nickname, **When** the owner updates it with nickname
   "The Hazy One", **Then** the update response and later fetches show that nickname.
2. **Given** a mug with a nickname, **When** the owner sends an update without a nickname (or
   with an empty/blank one), **Then** the nickname is cleared.
3. **Given** a mug owned by someone else, **When** a different member tries to set its
   nickname, **Then** the request is forbidden and the nickname is unchanged.
4. **Given** an owner submits a nickname longer than 50 characters, **When** it is submitted,
   **Then** it is rejected as an invalid request.

---

### Edge Cases

- A mug with no fills returns a fill count of 0, not an empty value.
- Two fills recorded at nearly the same time are both counted; nothing is lost.
- Fills can be backdated (a past fill date), but not dated in the future.
- Deleting a mug removes its fills and log entries along with it (existing behavior).
- Leading/trailing whitespace in a nickname is trimmed; a nickname that is only whitespace is
  treated as no nickname.
- A price of exactly 0 is allowed (e.g., a free pour).
- Prices are stored and returned to the cent (two decimal places); more precise input is
  rejected as invalid.
- Fetching history for a mug that does not exist returns "not found" (existing behavior).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every mug returned by the API (single mug, all mugs, owner's mugs, create, update)
  MUST include a fill count: a whole number ≥ 0.
- **FR-002**: The fill count MUST equal the number of fills recorded for that mug. Tasting
  log entries MUST NOT count as fills.
- **FR-002a**: The mug's owner MUST be able to record a fill on their mug, giving the amount
  poured in ounces (required) and the fill date (optional, defaults to today's date in New York
  time).
- **FR-002b**: Recording a fill MUST require login, and MUST be rejected as forbidden for
  anyone other than the mug's owner.
- **FR-002c**: Fill amount MUST be greater than 0 and no more than the mug's size; fill date
  MUST NOT be after today's date in the furthest-ahead time zone (UTC+14), so a user anywhere can
  record their own local date. Invalid fills MUST be rejected and not saved.
- **FR-002d**: A recorded fill MUST be returned with its id, mug id, id of the user who
  recorded it, fill date, amount in ounces, and creation timestamp.
- **FR-003**: Every tasting log entry returned MUST include: id, mug id, id of the user who
  added it, entry text, price (optional), and creation timestamp.
- **FR-004**: When adding a tasting log entry, the owner MUST be able to supply an optional
  price, which MUST be ≥ 0 with at most two decimal places.
- **FR-005**: Tasting log entries MUST be returned newest first.
- **FR-006**: Tasting log entries created before this feature MUST remain readable, with no
  price.
- **FR-007**: Every mug returned by the API MUST include its nickname; the value is null when no
  nickname is set.
- **FR-008**: Only the mug's owner MUST be able to set, change, or clear its nickname, through
  the existing mug update operation.
- **FR-009**: A nickname MUST be 1–50 characters after trimming whitespace; an empty or blank
  nickname in an update clears it, and an absent (or null) nickname leaves it unchanged.
- **FR-010**: Existing mugs MUST keep working with no nickname after the change; no existing
  data may be lost.
- **FR-011**: Changes to existing response shapes MUST be additive only, so current API clients
  keep working.

### Key Entities

- **Mug**: A member's club mug. Gains a **nickname** (optional, owner-editable) and a
  **fill count** (read-only, derived).
- **Tasting Log Entry** (mug history): One entry on a mug's log — what was poured, who added it,
  when. Gains an optional **price**.
- **Fill**: One recorded fill of a mug — who recorded it, the fill date, and ounces poured.
  Separate from tasting log entries, and carries no price (price lives only on tasting log
  entries). A mug has many fills; its fill count is how many it has.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For 100% of mugs, the fill count shown in the app matches the number of recorded
  fills for that mug, immediately after a fill is recorded.
- **SC-001a**: An owner can record a fill in a single action from the app.
- **SC-002**: The mug detail page shows a populated tasting log (text, price, author, date) for
  every mug that has entries, with no "0" or blank placeholders caused by missing data.
- **SC-003**: Nicknames set by owners are still shown after a page refresh and on other
  devices, 100% of the time.
- **SC-004**: Existing frontend and API clients continue to work unchanged after release (no
  regressions in current mug and history features).
- **SC-005**: Pages showing a list of mugs load no slower than before, as perceived by users.

## Assumptions

- Scope is limited to `plan.md` items 1–3 (fill count, tasting log fields including price,
  nickname), plus a way to record fills, which the fill count needs. Owner name, mug number, tier, and stickers (items 4–7) are out of scope.
- All timestamps are stored and returned in GMT/UTC. "Today" for fill dates means the current
  date in New York (America/New_York), which handles daylight saving automatically.
- The API does not localize times; the frontend converts UTC timestamps to the user's local time
  for display, and sends `fillDate` explicitly when the user's local day should be used.
- Price is in a single currency (the club's local currency); no currency field is needed.
- Price is optional on new entries so existing clients that don't send it keep working.
- Only the mug's owner records fills (matching who can add tasting log entries); staff or
  bartender roles do not exist yet.
- Listing, editing, or deleting individual fills is out of scope; only recording a fill and
  reading the count are included.
- The frontend will be updated to call the new "record a fill" action; until then fill counts
  stay at 0.
- Nickname is set only via the existing update operation, not at mug creation.
- An update without a nickname keeps the stored one, so clients that predate nicknames cannot
  erase it; the frontend sends an empty string to clear it.
- Reading mugs and history keeps its current access rules (single mug and history are public;
  lists require login).
