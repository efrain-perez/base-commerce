# Gila Commerce — E-Commerce Code Challenge

Enterprise-grade e-commerce app: Product CRUD, CSV import, search, and purchase, built as a Spring Boot API + React SPA, run together via Docker Compose.

> This file stays short on purpose — a fast orientation for a reviewer, not a transcript of every decision. Every non-obvious choice, its alternatives, and why they lost is written up in full in **[`docs/DECISIONS.md`](docs/DECISIONS.md)**, expanded incrementally as decisions were made during development rather than backfilled at the end.

## Status

Feature-complete. Started 2026-09-15, due 2026-09-23. All required phases are done — Product CRUD, search, CSV import + audit trail, cart/checkout, React frontend, and Docker Compose deployment with published images. Remaining time is going toward polish/hardening and whatever's left in [Future improvements](#future-improvements) below, which are deliberate scope cuts, not unfinished work.

## Running locally

### Full stack via Docker Compose
```
docker compose up
```
From the repo root. Pulls prebuilt images from GHCR and starts all three services — visit `http://localhost:8080`. Wiring details: [`docs/DECISIONS.md`](docs/DECISIONS.md#deployment-docker-compose).

Images are already published and public on GHCR via `.github/workflows/publish-images.yml`, which rebuilds and republishes them on every push to `main` — no setup needed before running the command above ([why GHCR, and the one manual visibility step behind it](docs/DECISIONS.md#image-hosting-github-container-registry-ghcr--github-actions)).

**No network access to GHCR, or don't want to depend on it?**
```
docker compose up --build
```
Builds both images locally from their `Dockerfile`s instead of pulling — no local JDK/Node toolchain needed either way, the build happens entirely inside Docker. See [`docs/DECISIONS.md`](docs/DECISIONS.md#image-hosting-github-container-registry-ghcr--github-actions) for why this fallback exists.

### Individual services, for active development

**Backend** (from `backend/`):
```
./gradlew bootRun
```
Requires Docker Desktop running — auto-starts a dev Postgres container. Serves the API on `http://localhost:8080`.

**Frontend** (from `frontend/`):
```
npm install
npm run dev
```
Serves the app on `http://localhost:5173`, proxying API calls to the backend on port 8080.

## Tech stack

| Layer    | Choice                                              |
|----------|------------------------------------------------------|
| Backend  | Spring Boot, layered (Controller → Service → Repository), DTOs separate from entities |
| Database | PostgreSQL + Flyway (versioned migrations)           |
| Frontend | React SPA (Vite + TypeScript, TanStack Query), not server-rendered |
| Runtime  | Docker Compose (postgres + backend + frontend/nginx) |

## Repository structure

```
gila_software/
├── backend/    Spring Boot API (+ Dockerfile)
├── frontend/   React SPA, Vite + TypeScript (+ Dockerfile, nginx.conf)
├── docs/       Challenge CSV + DECISIONS.md (full design rationale)
├── .github/workflows/   GHCR image publishing (push to main)
└── docker-compose.yml   3-service stack (postgres + backend + frontend/nginx)
```

Single monorepo, not separate repos + git submodules — one clone, one command. Why: [`docs/DECISIONS.md`](docs/DECISIONS.md#repository-structure).

## Key decisions at a glance

Each links to its full write-up — alternatives considered, why they lost, and any gotchas found along the way.

| Area | Choice | Why (short version) |
|---|---|---|
| [Product delete](docs/DECISIONS.md#product-delete-soft-delete) | Soft delete | Cart lines referencing it stay valid instead of dangling |
| [Product versioning](docs/DECISIONS.md#product-versioning-version-optimistic-locking) | `@Version` optimistic locking | Prevents concurrent-purchase stock races; same field detects price drift |
| [CSV import](docs/DECISIONS.md#csv-import-as-an-auditable-event-not-just-a-data-load) | Per-row upsert by SKU, isolated `REQUIRES_NEW` transactions | One bad row can't sink the batch or poison others |
| [Category](docs/DECISIONS.md#category-plain-string-column-not-an-enum-or-separate-table) | Plain string column | CSV-driven data can't be blocked by an enum mismatch |
| [SKU](docs/DECISIONS.md#sku-unique-immutable-after-creation-reusable-once-retired) | Unique, immutable, reusable after soft delete | Keeps import matching stable; retired SKUs aren't lost forever |
| [Search](docs/DECISIONS.md#search-case-insensitive-partial-match-paginated) | Native `ILIKE` + trigram index | Case-insensitive substring search that's actually indexed |
| [Cart](docs/DECISIONS.md#purchase-cart-based-checkout-server-side-persistence-no-auth) | Server-side, anonymous `HttpOnly` cookie | Survives refresh, no accounts needed, server-authoritative at checkout |
| [Cart stock validation](docs/DECISIONS.md#cart--checkout) | Flag insufficient stock, never silently rewrite quantity | Shopper's choice stays theirs; a restock self-heals it on next read |
| [Checkout](docs/DECISIONS.md#purchase-cart-based-checkout-server-side-persistence-no-auth) | All-or-nothing transaction + optimistic locking | Correctness over throughput; a half-completed purchase is the worst outcome |
| [Deployment](docs/DECISIONS.md#deployment-docker-compose) | Docker Compose, GHCR images, local `--build` fallback | One command for a reviewer, no dependency on the registry being reachable |
| [API conventions](docs/DECISIONS.md#api-conventions) | Unversioned paths, default `Page<T>` shape, RFC 7807 errors | One consumer, no reason to add ceremony a real standard already covers |
| [Testing](docs/DECISIONS.md#testing-strategy) | H2 + service/integration tests; Testcontainers deferred | Effort concentrated on import/checkout/search logic, not CRUD boilerplate |

## CSV import

Example file provided by the challenge, saved at `docs/Code Challenge E-Commerce.csv`. **Downloaded 2026-09-18.** Full validation results against the real file — 97 rows including a SQL injection attempt, an XSS payload, and malformed data, all handled correctly — are in [`docs/DECISIONS.md`](docs/DECISIONS.md#real-file-validation).

## Future improvements

Deliberately out of scope for the deadline (time permitting), in priority order (top addressed first if there's time). Full rationale for each: [`docs/DECISIONS.md`](docs/DECISIONS.md#future-improvements).

1. Async CSV import (background job + polling, for supplier-sized files)
2. Product images (not in the challenge's schema/CSV; needs a storage decision)
3. User accounts (real login/registration) — enables durable per-shopper data like order history and saved shipping addresses, currently anonymous-cookie-only or entirely absent

## AI usage disclosure

AI (Claude Code) was used during development to accelerate implementation and pressure-test design decisions. Per the challenge's explicit rule, AI-generated code comments are removed before submission.
