# Quickstart: Validate Mug Fill Count, Tasting Log Details, and Nicknames

See [contracts/api-specification.delta.yml](contracts/api-specification.delta.yml) for request and
response shapes and [data-model.md](data-model.md) for validation rules.

## Prerequisites

- Java 21, Docker
- A Supabase access token for a test user (`TOKEN`) and that user's id (`USER_ID`, the JWT `sub`)
- A second user's token (`OTHER_TOKEN`) for forbidden-path checks

## 1. Automated checks

```bash
./gradlew build          # regenerates models, compiles, runs all Spock specs
docker build -t mugs:dev .
```

Expected: build succeeds; `MugControllerSpec`, `MugServiceSpec`, and
`MugRepositoryImplementationSpec` pass.

## 2. Upgrade an existing database

```bash
cp mugs.db /tmp/mugs-upgrade.db
SPRING_DATASOURCE_URL=jdbc:sqlite:/tmp/mugs-upgrade.db ./gradlew bootRun
```

Expected: app starts; existing mugs are returned with `nickname: null` and their real
`fillCount`; existing history entries have `price: null`. Restart once more: the app starts
again with no schema errors (the upgrade is safe to repeat).

## 3. End-to-end scenarios (app on `localhost:8080`)

```bash
API=http://localhost:8080/api/mugs
AUTH="Authorization: Bearer $TOKEN"
```

| # | Action | Expected |
|---|--------|----------|
| 1 | `POST $API` (12 oz mug) | 201/200, `fillCount: 0`, `nickname: null`. Save `MUG` id. |
| 2 | `POST $API/$MUG/fills` `{"amountOunces":12,"fillDate":"<today>"}` | 201, `fillDate` = the date sent, `createdAt` ends in `Z` (UTC), `filledByUserId` = `USER_ID` |
| 3 | `GET $API/$MUG` (no auth) | `fillCount: 1` |
| 4 | `POST $API/$MUG/fills` `{"amountOunces":13}` | 400 (over mug size) |
| 5 | `POST $API/$MUG/fills` `{"amountOunces":0}` | 400 |
| 6 | `POST $API/$MUG/fills` with a `fillDate` two days ahead | 400 (a date already reached in UTC+14 is accepted) |
| 7 | Step 2 with `OTHER_TOKEN` | 403; `fillCount` unchanged |
| 8 | Step 2 with no token | 401 |
| 9 | `POST $API/unknown-id/fills` | 404 |
| 10 | `POST $API/$MUG/history` `{"entry":"Hazy IPA","price":6.50}` | 201 with `price: 6.5`; `fillCount` still 1 |
| 11 | `POST $API/$MUG/history` `{"entry":"Stout"}` | 201 with `price: null` |
| 12 | `POST $API/$MUG/history` with `price: -1` or `6.505` | 400 |
| 13 | `GET $API/$MUG/history` | Both entries, newest first, all fields present |
| 14 | `PUT $API/$MUG` with `"nickname":"  The Hazy One "` | 200, `nickname: "The Hazy One"` |
| 15 | `PUT $API/$MUG` without `nickname` | 200, `nickname: null` |
| 16 | `PUT $API/$MUG` with a 51-char nickname | 400 |
| 16a | `GET $API` with no token | 401 |
| 17 | `GET $API` and `GET $API/owners/$USER_ID` | Each mug has its own correct `fillCount` |
| 18 | `DELETE $API/$MUG` | 204; its fills and history are gone |
