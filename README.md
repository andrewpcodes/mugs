# mugs

Spring Boot API for managing mugs, packaged for Docker and Raspberry Pi deployment.

## Docker run

Build the image:

```bash
docker build -t mugs:latest .
```

Run it with a persistent SQLite volume:

```bash
docker run -d \
  --name mugs \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:sqlite:/data/mugs.db \
  -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://jrtcojpgdubbmgsmnhgs.supabase.co/auth/v1 \
  -e APP_SECURITY_JWT_AUDIENCE=authenticated \
  -v mugs-data:/data \
  --restart unless-stopped \
  mugs:latest
```

## Docker Compose

Start the app with Compose:

```bash
docker compose up -d --build
```

The SQLite database is stored in the `mugs-data` Docker volume at `/data/mugs.db`.

## GitHub Container Registry

This repository is configured to publish Docker images to GitHub Container Registry (GHCR) from GitHub Actions.

Published images use the name:

```text
ghcr.io/<your-github-organization-or-user>/mugs
```

Typical tags include:

- `latest` for the default branch
- the branch name for branch builds
- the Git commit SHA for traceability
- release tags like `v1.2.3`

To run the published image on a Raspberry Pi:

```bash
docker run -d \
  --name mugs \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:sqlite:/data/mugs.db \
  -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://jrtcojpgdubbmgsmnhgs.supabase.co/auth/v1 \
  -e APP_SECURITY_JWT_AUDIENCE=authenticated \
  -v mugs-data:/data \
  --restart unless-stopped \
  ghcr.io/<your-github-organization-or-user>/mugs:latest
```

## Raspberry Pi notes

- Use a Raspberry Pi OS image with Docker installed.
- The Dockerfile uses multi-arch base images, so it can run on common Raspberry Pi ARM variants.
- If you build on another machine and want to target the Pi explicitly, use Docker Buildx:

```bash
docker buildx build --platform linux/arm64 -t ghcr.io/<your-github-organization-or-user>/mugs:latest --push .
```

- If you want to publish manually to GHCR, keep the image name in the `ghcr.io/<owner>/mugs` namespace.

## Configuration

Environment variables you may want to set in production:

- `SPRING_DATASOURCE_URL` — defaults to `jdbc:sqlite:/data/mugs.db`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` — Supabase or other JWT issuer
- `APP_SECURITY_JWT_AUDIENCE` — expected JWT audience
- `SERVER_PORT` — HTTP port, defaults to `8080`
- `APP_TIME_LOCAL_ZONE` — time zone that decides which calendar day counts as "today" for fill
  dates, defaults to `America/New_York`
- `TZ` — set to `UTC` in the image; all API timestamps are stored and returned in UTC, and clients
  convert them to local time for display

## API model generation

The API contract is defined in `api-specification.yml`. This file drives both generated Java models and Swagger UI documentation.

- The generated Java models are written to `build/generated/openapi/src/main/java/com/overmild/mugs/models`.
- `compileJava` runs `openApiGenerate` automatically, so a normal build always regenerates models from the spec.
- The same `api-specification.yml` is copied into app static resources at build time and served at `/api-specification.yml` for Swagger UI.

You can run generation directly with:

```bash
./gradlew openApiGenerate
```

