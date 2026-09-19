# Gila Commerce — E-Commerce Code Challenge

Enterprise-grade e-commerce app: Product CRUD, CSV import, search, and purchase, built as a Spring Boot API + React SPA, run together via Docker Compose.

> This README is written and expanded as decisions are made, not backfilled at the end — the challenge is explicitly graded on the design judgment behind the code, not just the working CRUD. Every non-obvious choice below includes the alternative(s) considered and why it lost.

## Status

Work in progress. Started 2026-09-15, due 2026-09-23. Phase 0 (foundations), Phase 1 (Product CRUD, search, CSV import, audit trail), Phase 2 (cart, checkout), and Phase 3 (React frontend) complete. Phase 4 (Docker Compose wiring all three services) remains.

## Tech stack

| Layer    | Choice                                              |
|----------|------------------------------------------------------|
| Backend  | Spring Boot, layered (Controller → Service → Repository), DTOs separate from entities |
| Database | PostgreSQL + Flyway (versioned migrations)           |
| Frontend | React SPA (separate from backend, not server-rendered) |
| Runtime  | Docker Compose (postgres + backend + frontend/nginx) |

### Backend package structure: by-layer
Confirmed 2026-09-16. Top-level `controller/`, `service/`, `repository/`, `dto/`, `entity/` packages (classic 3-tier), rather than grouping by feature (`product/`, `cart/`, `importjob/`, each internally layered). By-feature is the more common convention for apps with several largely-independent domains, but by-layer was chosen as the style Efrain is most fluent in and can explain most clearly in an interview setting — a deliberate tradeoff, not an oversight, given this project's small number of domains keeps by-layer's main downside (jumping across packages per feature) manageable.

### Spring profiles across environments
Confirmed 2026-09-16. Base `application.yaml` holds environment-agnostic settings only. `application-dev.yaml` (Postgres at `localhost`, matching `backend/compose.yaml`, auto-started by the `spring-boot-docker-compose` dependency) is active for local/IDE runs. `application-test.yaml` (H2 in-memory) is used for tests. For the full Docker Compose deployment (Phase 4), the Postgres connection is configured via **environment variables set directly in the root `docker-compose.yml`** (`SPRING_DATASOURCE_URL`, etc., pointed at the `postgres` service hostname) rather than a dedicated `application-docker.yaml`.

**Alternative considered:** a dedicated `application-docker.yaml` profile file. Rejected in favor of env vars — one less file to keep in sync, it's the standard way to parameterize a containerized Spring Boot app, and it gives whoever runs the compose file the flexibility to override connection details (e.g. pointing at an external DB) without editing and rebuilding the image.

### Two `compose.yaml` files, on purpose

The repo has (or will have) two Docker Compose files at different levels — not a misplaced file:

- **`backend/compose.yaml`** — the Spring Initializr default, kept because of the `spring-boot-docker-compose` (`developmentOnly`) dependency. That feature auto-starts/stops a bare Postgres container when the app is run locally from IntelliJ/Gradle, and it looks for `compose.yaml` in the process's working directory, which for a Gradle module run is `backend/`. It only backs local dev (matches `application-dev.yaml`'s `localhost:5432`) and is irrelevant to the shipped deployment.
- **Root `docker-compose.yml`** (Phase 4) — the full 3-service stack (postgres + backend + frontend/nginx) a reviewer runs with a single `docker compose up`. This belongs at the repo root since it spans both `backend/` and `frontend/`.

### Flyway autoconfiguration gotcha (Spring Boot 4)
Confirmed 2026-09-17, found while verifying Phase 0. Spring Boot 4 split the old monolithic `spring-boot-autoconfigure` jar into many per-feature autoconfigure modules (`spring-boot-jdbc`, `spring-boot-jpa`, `spring-boot-hibernate`, etc.). Depending on `flyway-core`/`flyway-database-postgresql` alone is **not** enough to get migrations running — `FlywayAutoConfiguration` itself now lives in a separate `org.springframework.boot:spring-boot-flyway` module, which nothing else pulls in transitively. Without it, Spring Boot silently never creates a `Flyway` bean: no error, no log line, app starts "successfully" against an empty schema (masked further by `ddl-auto: validate` having nothing to validate before any `@Entity` exists). Caught by manually inspecting the Postgres container (`\dt` showed zero tables) after a clean `bootRun`. Fixed by adding `implementation 'org.springframework.boot:spring-boot-flyway'` to `build.gradle`.

**Follow-on: Flyway disabled for the `test` profile.** Once Flyway actually started running, it also ran against the H2 test datasource and failed — `V1__init.sql` uses Postgres-only syntax (`CREATE EXTENSION`, `GIN ... gin_trgm_ops`) that H2 doesn't support. `application-test.yaml` now sets `spring.flyway.enabled: false` and `hibernate.ddl-auto: create-drop`, so tests get their schema generated straight from the `@Entity` classes instead of running the Postgres migration. Consistent with the existing H2-over-Testcontainers tradeoff already noted under [Testing strategy](#testing-strategy) — full Flyway-against-real-Postgres coverage is what Testcontainers would close, deferred for the same reason.

### Spring Boot 4 / Jackson 3 package-split gotchas
Found while implementing Phase 1 (Product CRUD/search/import). Spring Boot 4.1.1 pulls in **Jackson 3** (group `tools.jackson.*`, not the legacy `com.fasterxml.jackson.*`) and has further split its test-support jars per feature, same pattern as the Flyway autoconfiguration split above. Two concrete traps hit during this phase:
- `ObjectMapper` in test code is `tools.jackson.databind.ObjectMapper`, not `com.fasterxml.jackson.databind.ObjectMapper` — the old import compiles-fails immediately, which at least fails loud rather than silent.
- `@AutoConfigureMockMvc` now lives in `org.springframework.boot.webmvc.test.autoconfigure`, not `org.springframework.boot.test.autoconfigure.web.servlet` — `spring-boot-starter-webmvc-test` does pull the right module (`spring-boot-webmvc-test`) transitively, it's just under a new package.
- Separately: adding a new `@ExceptionHandler(MaxUploadSizeExceededException.class)` method to `GlobalExceptionHandler` broke **application startup entirely** (`Ambiguous @ExceptionHandler method mapped` — `ExceptionHandlerMethodResolver`), because `ResponseEntityExceptionHandler`'s own internal `handleException(...)` dispatcher already declares a handler for that exact exception type as part of its built-in Spring MVC exception list. Fixed the same way `handleMethodArgumentNotValid` was already being customized: `@Override protected handleMaxUploadSizeExceededException(...)` instead of a new sibling `@ExceptionHandler` method. Worth remembering for any future exception type added to this handler — check whether `ResponseEntityExceptionHandler` already owns it before adding a new `@ExceptionHandler` method rather than overriding.

### Flyway migration convention
Confirmed 2026-09-16. Standard `V{version}__{description}.sql` naming, but as a **single consolidated initial migration** covering the full schema rather than one migration per feature/phase. Migration granularity isn't a focus area for this challenge, so the simpler option was chosen over modeling schema evolution through migration history.

## Repository structure

```
gila_software/
├── backend/    Spring Boot API (+ Dockerfile)
├── frontend/   React SPA, Vite + TypeScript (+ Dockerfile, nginx.conf)
├── docs/       Challenge PDF + planning notes
├── .github/workflows/   GHCR image publishing (push to main)
└── docker-compose.yml   3-service stack (postgres + backend + frontend/nginx)
```

**Decision: single monorepo, not two repos + git submodules.** A reviewer needs one `git clone` and one command to get the whole app running. Submodules require `--recurse-submodules` or the frontend silently ends up empty — a footgun with no upside at this project's scale (one developer, no independent release cadence between FE/BE). Submodules earn their complexity when frontend and backend are owned by different teams with different deploy schedules; that's not this project.

## Architecture & design decisions

### Product versioning (`@Version` optimistic locking)
One field serves two purposes: it prevents concurrent-purchase stock races (two users buying the last unit), and the same version number lets the app detect and surface price/detail changes on items sitting in a cart. `OrderItem` stores a snapshot (`product_id`, `name`, `price_at_purchase`, `version_at_purchase`) rather than a full product-version-history table — enough to answer "user X bought Y at version Z for price P" without extra scope.

**Alternative considered:** a dedicated `ProductVersion` history table. Rejected as unnecessary scope for what the challenge asks — the snapshot-on-order approach answers the only questions that actually matter (what did the buyer see, what did they pay) without maintaining a full audit trail of every edit.

### Product delete: soft delete
Confirmed 2026-09-16. `Product` gets a `deleted_at` (or `status`) field rather than a hard `DELETE`. A hard delete would leave live `CartItem` rows pointing at a nonexistent product (unlike `OrderItem`, which is already a safe point-in-time snapshot and unaffected either way). Deleted products are excluded from search/listing/CRUD-read by default but the row — and its id — stays valid for any cart that already referenced it, so checkout can still detect and reject it cleanly instead of hitting a dangling FK.

**Alternative considered:** hard delete. Rejected — would require either blocking deletion of any product sitting in an active cart (extra bookkeeping, worse UX) or cascading the delete into `CartItem` (silently mutates another user's cart). Soft delete avoids both without added complexity.

### Product change audit trail
Confirmed 2026-09-17, added mid-Phase-1 at Efrain's explicit request that product auditing be a first-class part of the design, not an afterthought. A `product_audit` table stores a full snapshot of a product's **catalog/identity attributes** (`sku`, `name`, `description`, `category`, `price`, `weight_kg`, plus `version` and an `action` of `CREATED`/`UPDATED`/`DELETED`) every time `ProductService.create`/`update`/`softDelete` runs, and every time the CSV importer upserts a row (stamped with the triggering `import_job_id`). Exposed read-only via `GET /products/{id}/audit` (paginated), mirroring the `GET /import-jobs/{id}` precedent.

**`stock` is deliberately excluded from the audit snapshot.** Refined 2026-09-17 (the table originally included it). Stock is operational — it changes on every sale — not a meaningful "edit" to the product's catalog identity the way renaming it or changing its price is. Auditing it would mean a single popular product accumulates one audit row per unit ever sold across its lifetime, which is a fundamentally different (and much higher-volume) kind of history than "who changed this product's name/price/category and when." Consequently, **checkout writes nothing to `product_audit`** — the only thing a purchase changes on a `Product` (`stock`) isn't in scope for this table at all; stock's full transactional history is already fully reconstructable from `Order`/`OrderItem` (see [Purchase](#purchase-cart-based-checkout-server-side-persistence-no-auth) below).

### CSV import as an auditable event, not just a data load
`ImportJob` (`file_name`, `executed_at`, `total_rows`, `success_count`, `failure_count`, `status`) + `ImportJobError` (`import_job_id`, `row_number`, `raw_data`, `error_reason`). A bad row fails and is logged individually; the rest of the import continues rather than aborting the whole file on one bad line.

- **Duplicate SKU on import → upsert by SKU.** Re-importing a CSV with an existing SKU updates that product's fields and bumps `@Version`, which feeds directly into the price-change-detection design above instead of needing separate logic.
- **Invalid row → fail only that row**, continue processing the rest, report failures via `ImportJobError`.

**Alternative considered:** reject the whole file on any bad row (fail-fast). Rejected — a single malformed row in a large supplier CSV shouldn't block importing the other 999 good rows; the audit trail lets someone fix and re-run just the failures.

**Per-row `REQUIRES_NEW` transaction, not one transaction for the whole file.** Postgres aborts the *entire current transaction* on any statement error until a `ROLLBACK` — if the whole CSV import ran inside one `@Transactional` method, one bad row (e.g. a unique-constraint violation) would poison every row processed after it in that same transaction, even though nothing was wrong with them. `ProductImportRowProcessor.upsert(...)` (a separate Spring bean, required so the AOP proxy for `@Transactional(propagation = REQUIRES_NEW)` actually applies — self-invocation on the same bean bypasses the proxy) runs each row's write in its own transaction, so a failed row rolls back in isolation. This is a genuinely Postgres-specific correctness requirement that the H2 test suite alone cannot prove (H2 doesn't necessarily abort the whole transaction the same way) — documented here so it isn't "simplified" back into a single transaction later because the tests still pass without it.

**Upload & execution: multipart file upload, processed synchronously.** Confirmed 2026-09-16. The UI uploads the CSV via a standard multipart endpoint; the server processes it inline and returns the completed `ImportJob` summary (counts, per-row errors) in the response — no background job/polling. Sized appropriately for a challenge-scale CSV, and keeps the frontend simple (no job-status screen). Async processing (return a `jobId` immediately, process on a background thread, frontend polls for status) was considered and is the more scalable answer for very large files, but adds scope nothing in the challenge calls for — see [Future improvements](#future-improvements-time-permitting).

Spring Boot's default multipart limit is 1MB, which would silently reject any realistically-sized CSV with a generic error — `application.yaml` raises `spring.servlet.multipart.max-file-size`/`max-request-size` to 10MB, with a `MaxUploadSizeExceededException` → 413 `ProblemDetail` handler so an oversized file fails with a clear message instead of a raw stack trace.

**Import job history is a read-only API, not deferred.** `GET /import-jobs` (paginated) and `GET /import-jobs/{id}` (job + its row-level errors) ship in Phase 1 rather than being pushed to "time permitting" — the audit trail this feature is built around (see above) isn't very useful if it can only be seen once, inline in the upload response.

### Category: plain string column, not an enum or separate table
Confirmed 2026-09-16. The CSV spec itself hedges (`category (string/enum)`), so `Product.category` is a plain indexed string — filtering by category works the same as on a normalized table, and CSV import can never fail because of an unrecognized category value.

**Alternatives considered:** a fixed Java `enum` was rejected — the challenge is explicitly a CSV-driven import feature, and hardcoding valid categories means any real-world CSV value outside that set becomes an avoidable import failure. A separate `Category` entity (products FK'd to it, auto-created on first sight) was the preferred design on normalization grounds — it's the more "textbook enterprise" answer and would support category management later — but nothing in the challenge asks for categories to be managed independently of products, so it was set aside in favor of the simpler option given the timeline.

### SKU: unique, immutable after creation, reusable once retired
Confirmed 2026-09-16 (base rule), refined 2026-09-17. `sku` has a DB-level unique index (required regardless, since CSV import upserts by SKU). It's also treated as a set-once business key: present on product creation, but not editable via the update endpoint (`ProductUpdateRequest` simply has no `sku` field). CSV import matches products by SKU — if SKU were editable, changing it on an existing product would desync it from future re-imports.

**Retired SKUs are reusable.** Because `Product` is soft-deleted, the uniqueness constraint is a **partial unique index** — `CREATE UNIQUE INDEX uq_product_sku ON product (sku) WHERE deleted_at IS NULL` — rather than a plain column-level `UNIQUE`. This mirrors the application-level uniqueness check (`existsBySku`/`findBySku`, both filtered by Hibernate's `@SQLRestriction("deleted_at IS NULL")` on `Product`) exactly, so soft-deleting a product frees its SKU for reuse by a later create or CSV import row, instead of permanently retiring it. A plain unique constraint was the initial approach but was revised once the interaction with soft delete was worked through — the plain-constraint version would have let the app-level check say "SKU available" while the DB rejected the insert with a raw constraint violation.

### Price and stock data types
Confirmed 2026-09-16. `price` is `BigDecimal`, never `float`/`double` — binary floating-point can't represent currency exactly (e.g. `0.1 + 0.2 != 0.3`), a real correctness risk for checkout totals and stock-value math. `stock` (CSV column is ambiguously typed as "string/int") is parsed to `int` on import; negative or non-numeric values fail that row individually via the existing `ImportJobError` mechanism rather than needing separate validation machinery.

### Search: case-insensitive, partial match, paginated
Matches how users actually search a product catalog (`"lap"` finds `"Laptop"`), implemented with a `Pageable`-based repository query.

**Alternative considered:** Postgres full-text search (`tsvector`/`tsquery`) with relevance ranking. More capable, but more scope than this challenge calls for — noted here as a considered-and-deferred option rather than an oversight.

**Trigram index on `product.name`.** A plain B-tree index (like the one backing the SKU unique index) only accelerates prefix matches (`LIKE 'lap%'`), not the substring `ILIKE '%lap%'` queries this search actually runs — those would otherwise always be a full table scan. `V1__init.sql` enables the `pg_trgm` extension and adds a `GIN` trigram index on `product.name` so the substring search has an index to use. Won't be observably faster at this challenge's data volume, but it's a low-risk, one-line addition that matches the "enterprise-grade" intent behind the search feature rather than leaving an unindexed `ILIKE` in place.

**Native `ILIKE` query instead of a derived Spring Data method.** `ProductRepository.search(...)` is a native `@Query` running `name ILIKE '%term%'` directly against the raw `name` column, not the more idiomatic-looking `findByNameContainingIgnoreCase(...)`. The derived method generates `lower(name) LIKE lower('%term%')`, which does not match the plain trigram index above (the index is built on `name`, not `lower(name)`) — it would silently fall back to a full table scan despite the index existing. Native `ILIKE` is what Postgres's `pg_trgm` operator support actually optimizes for. Cost of this choice: the query can't safely honor an arbitrary client-supplied `?sort=`, so `GET /products` always orders by `id` (needed anyway for deterministic pagination — `LIMIT`/`OFFSET` with no `ORDER BY` has undefined row order in Postgres). Client-configurable sort on this endpoint is listed under [Future improvements](#future-improvements-time-permitting). `GET /import-jobs` has no such restriction since it's a plain derived query and supports `sort` normally.

Because `@SQLRestriction("deleted_at IS NULL")` on `Product` only rewrites Hibernate-generated SQL (derived/JPQL queries), not native ones, the native search query includes `deleted_at IS NULL` explicitly — otherwise soft-deleted products would leak back into search results. Covered by a dedicated test (`ProductRepositorySearchIT`) seeding a soft-deleted product that matches the search term and asserting it's excluded.

### Purchase: cart-based checkout, server-side persistence, no auth
The app has no login/user accounts (not requested by the challenge), so the cart is anonymous but still persisted server-side rather than kept only in browser state — this survives page refreshes/tab loss and keeps checkout validation server-authoritative rather than trusting a client-submitted item list.

**Design:** `Cart` (`id`, `created_at`, `status`) + `CartItem` (`cart_id`, `product_id`, `quantity`, `added_at`). On first interaction, the backend issues a `cartId` (UUID) via an `HttpOnly` cookie — invisible to JS, sent automatically on every request, no manual token plumbing on the frontend.

**Endpoint shape is fully implicit — no cart ID ever appears in any URL or request body.** Confirmed 2026-09-18, correcting an early-planning sketch that had `POST /carts/{cartId}/checkout` with the ID in the path. That shape is incompatible with an `HttpOnly` cookie: JavaScript can never read the cookie to build that URL in the first place. The actual API resolves "whose cart" purely from the incoming cookie (still sent automatically by the browser on every request even though `HttpOnly` blocks JS from reading it): `GET /cart`, `POST /cart/items`, `PATCH /cart/items/{productId}`, `DELETE /cart/items/{productId}`, `POST /cart/checkout`, plus a read-only `GET /orders/{id}` for a post-checkout confirmation page to survive a refresh (mirrors the `GET /import-jobs/{id}` precedent). `POST /cart/items` **increments** an existing line's quantity (or creates one); `PATCH /cart/items/{productId}` **sets an exact quantity** — kept as two distinct endpoints so "add 2 more" and "I only want 1 now" aren't overloaded onto the same verb. Status codes match the existing Product convention exactly: `201` for the POST that adds an item (with a `Location` header), `200` + the updated cart for the PATCH, `204 No Content` for the DELETE.

**Checkout logic:** `POST /cart/checkout` reads the persisted `CartItem` rows (not a client-submitted list) inside a single `@Transactional` method: empty cart → `EmptyCartException` (400); for each line, reload the `Product` fresh, `InsufficientStockException` (409) if stock is short, decrement stock, and build an `OrderItem` snapshot (`product_id`, `name`, `price_at_purchase`, `version_at_purchase`, `quantity`); on success, save the `Order`+`OrderItem`s, clear the cart's items, and flip `Cart.status` to `CHECKED_OUT` (so the visitor's next add-to-cart correctly starts a fresh cart rather than resurrecting a finished one). No client-supplied version is needed to detect a product that changed underneath the cart — `Product`'s existing `@Version` column does this for free via the ordinary load→mutate→save cycle: two simultaneous checkouts touching the same product will have one `saveAndFlush` fail with `OptimisticLockingFailureException`, already mapped to 409 by the existing `GlobalExceptionHandler`. This concurrency protection was designed but is **not covered by an automated test** — H2 doesn't reliably reproduce real Postgres row-versioning semantics, so a two-thread race test would be flaky without proving much beyond the single-threaded optimistic-lock mapping Phase 1 already exercises; documented here instead, the same proportionality call made for the CSV import's Postgres-transaction-abort behavior.

**Checkout is all-or-nothing, deliberately the opposite of CSV import's per-row isolation.** CSV import intentionally isolates each row in its own `REQUIRES_NEW` transaction so one bad row doesn't sink an otherwise-good batch (see above). Checkout is the opposite: it's a single customer transaction with no `REQUIRES_NEW` anywhere, so any failure on any line — insufficient stock, a product having been deleted, or a version conflict — rolls back everything, including stock already decremented earlier in the same loop. A half-completed purchase would be a far worse outcome than a half-completed bulk import.

**A soft-deleted product still sitting in someone's cart is handled differently depending on where it's seen.** `GET /cart` silently drops that line (the subtotal reflects only what's actually purchasable) — a stale line shouldn't break the whole cart view. Checkout, by contrast, fails loudly with a 404 if it encounters one — checkout is a financial action and must never silently proceed or silently skip a line the shopper thinks they're paying for.

**`Cart` uses an application-assigned UUID primary key and implements `Persistable<UUID>`.** The first entity in this codebase without a DB-generated ID. Spring Data's default "is this new" check is "is `@Id` null," which is always false here since the UUID is set before the first save — without `Persistable`, every cart creation would silently route through `merge()` (an extra `SELECT` before the insert) instead of a direct `persist()`. A few extra lines (a transient `isNew` flag flipped via `@PrePersist`/`@PostLoad`) restore the direct-insert path — a well-known, textbook-correct pattern for manually-assigned IDs.

**Alternative considered:** client-side-only cart (React state + `localStorage`, full item list submitted at checkout). Rejected — it trusts the client for cart contents until the moment of checkout (a user could tamper with quantities/prices client-side before submitting, though server-side price/stock revalidation at checkout would still catch it) and loses the cart on a cleared browser or different device. Server-side persistence is the more defensible "enterprise-grade" choice and isn't meaningfully more work given `Cart`/`CartItem` are simple entities.

**No price/version snapshot on `cart_item`.** Considered and dropped 2026-09-18: an earlier ambition was to detect and surface a price change on an item sitting in a cart (e.g., "this went up since you added it"), which would need a `price_at_add` column to compare against. `GET /cart` instead always shows each item's live current price. Nothing in the challenge asks for this UX nicety, and the correctness-critical part — protecting checkout against a product changing concurrently — doesn't need it (see `@Version`, above).

**No payment or shipping details collected.** Confirmed 2026-09-17. The challenge spec explicitly says "purchase (fake payment)" and there's no user-account concept anywhere in this app, so there's no shipping address to collect and nothing to actually bill. Checkout validates stock/version and creates the `Order`/`OrderItem` records with a stubbed "approved" (`COMPLETED`) outcome — no payment form, no card fields, no address fields anywhere in the schema or API. A trivial "fake payment confirmation" step in the frontend UI (never wired to real processing) is listed under [Future improvements](#future-improvements-time-permitting) as a nice-to-have for demo polish.

### Image hosting: GitHub Container Registry (GHCR) + GitHub Actions
Confirmed 2026-09-16, wired up in Phase 4. Rather than requiring a reviewer to build both images locally, `.github/workflows/publish-images.yml` builds and pushes `ghcr.io/efrain-perez/base-commerce-backend:latest` and `ghcr.io/efrain-perez/base-commerce-frontend:latest` on every push to `main`. The root `docker-compose.yml` references those tags directly (no `build:` context), so `docker compose up` only pulls — no local JDK/Node toolchain needed, just Docker itself.

**Alternative considered:** Docker Hub. Also free for public images, but anonymous pulls are rate-limited (100/6h per IP), which risks failing for a reviewer on a shared network. GHCR ties to the same GitHub account/repo already used for submission, with no such limit and no separate account to set up.

**One-time manual step, not automatable from the workflow:** newly-published GHCR packages default to private visibility even from a public repo's workflow. After the first successful push/workflow run, the two packages need their visibility set to public once, in GitHub's package settings — otherwise a reviewer's `docker compose up` will fail to pull with an authentication error. This is a real sequencing dependency worth being explicit about rather than assuming it "just works" after the workflow goes green.

### Repo naming: `base-commerce`, deliberately generic
Confirmed 2026-09-18. The GitHub repository is named `base-commerce` (`github.com/efrain-perez/base-commerce`) rather than something Gila-specific — chosen so it reads as a generic full-stack e-commerce reference project on a GitHub profile rather than an obvious take-home-exercise artifact, while this README (and all in-code naming — `dev.efrain.gilacommerce`, `Gila Commerce` as the Spring app name) stays fully Gila-specific, since that context matters here and shouldn't be scrubbed from the actual deliverable.

### API conventions
Confirmed 2026-09-16, decided once up front so every controller follows the same shape rather than drifting endpoint-by-endpoint.

- **Base path: unversioned** (`/products`, `/carts`, ...), not `/api/v1/...`. API versioning earns its cost when independent external consumers can't upgrade in lockstep; here there's exactly one consumer (the bundled React app), deployed together with the backend, so a version prefix would signal a strategy this project doesn't actually need.
- **Pagination: Spring Data's default `Page<T>` JSON shape** (`content`, `totalElements`, `totalPages`, `number`, `size`, ...), used as-is rather than wrapped in a custom envelope — it's already what a `Pageable`-based query returns, and any Spring/React developer recognizes it immediately.
- **Errors: RFC 7807 (`application/problem+json`)** via Spring's built-in `ProblemDetail` support (no extra dependency). Used for both domain errors (e.g. product not found) and validation failures, instead of a hand-rolled `{ "error": ... }` shape — using a real standard here is a small, genuine correctness/consistency signal.

## Frontend

Phase 3, confirmed 2026-09-18. React SPA covering the challenge's three required UI areas (CRUD, Search, Purchase for Products), built as a separate app in `frontend/` talking to the backend over its existing unversioned JSON API — no backend changes were needed for this phase.

**Tooling: Vite + React + TypeScript, Tailwind CSS, React Router, TanStack Query, react-hook-form + zod.** Vite over Next.js — there's no SSR/SEO requirement here (a single anonymous-cart SPA), so a routing/data framework built around server rendering would be net-negative complexity. TanStack Query over hand-rolled `useEffect`/`fetch` state — the app is almost entirely server state (products, cart, orders) with real caching/invalidation needs (e.g. the nav cart badge and the Cart page sharing one query result), which is exactly what it's built for. `react-hot-toast` was added as one pragmatic dependency beyond this list — hand-rolling a toast/notification system (stacking, dismiss timers, a portal) is a time sink for a fully-solved problem at this point in the deadline.

**CSV import gets a UI, even though the challenge doesn't require one.** The PDF's "UI is required for" list names only CRUD, Search, and Purchase — CSV import is a required *feature* but not a required *UI*. A minimal upload panel (file picker + submit + a results table showing per-row success/failure) was built anyway, embedded in the Manage Products page, since leaving a fully-built backend feature only reachable via curl/Postman would read as an oversight rather than a deliberate cut in a demo. No import-job *history* browsing UI was built, though (`GET /import-jobs`) — the immediate result of the operation just run is enough, and that endpoint has no PDF-driven reason to need a UI at all.

**Two separate views instead of one unified product screen.** A **Shop** view (search, browse, add-to-cart — no edit/delete controls) and a separate **Manage Products** view (a table with inline create/edit/delete, plus the CSV import panel). There's no auth/roles anywhere in this app, so nothing technically prevents one unified screen, but a real storefront never shows delete buttons to shoppers — splitting them is purely an information-architecture call reflecting that browsing-to-buy and managing-the-catalog are different jobs, and it reads more like a real product in a demo.

**No automated frontend tests this phase.** Same time-boxed reasoning as other scope cuts already in this README (Testcontainers, async import, the concurrent-checkout race test) — verification is a full manual click-through of every required flow (search, CRUD with its 409/400 error paths, CSV import with a deliberately bad row, add-to-cart, quantity changes, checkout, order confirmation), documented here rather than silently skipped. The backend already carries the real automated test coverage; the frontend is comparatively thin glue code calling an already-tested API.

**Dev-time architecture: Vite's dev-server proxy, not CORS.** The cart's identity cookie is `HttpOnly`, `SameSite=Lax`, `Secure=false` (no HTTPS anywhere in this project's scope — see [Purchase](#purchase-cart-based-checkout-server-side-persistence-no-auth)). A cross-origin dev setup (Vite's default port talking to the backend on 8080) would need CORS configured on the backend **and** the cookie relaxed to `SameSite=None`, which in turn requires `Secure=true`/HTTPS — undoing an already-made decision just to satisfy local dev. Instead, `vite.config.ts` proxies `/products`, `/cart`, `/orders`, `/import-jobs` through to the backend, so the browser sees everything as same-origin even in dev — exactly how the Phase 4 nginx setup will front both services in production. Net effect: zero CORS configuration exists anywhere in this project, in dev or prod.

**A dedicated checkout review step, not Cart → Order Confirmation directly.** Confirmed 2026-09-18, added after the first click-through made "Cart" jumping straight to a completed order feel like a skipped step — real checkouts always have a distinct moment where the shopper reviews and commits, even a fake-payment one. `/checkout` is a read-only summary (items, subtotal, an explicit "this is a demo checkout, no payment is collected" note) with a "Place Order" button — that button, not the Cart page's "Proceed to Checkout," is what actually calls `POST /cart/checkout`. This is the "fake payment confirmation" idea originally noted as a deferred nice-to-have — pulled into scope once the gap became obvious in actual use, rather than left as a stretch item.

**Post-checkout cart state.** Checkout flips the just-used cart to `CHECKED_OUT` and clears its items, but a replacement cart is only ever lazily created by the backend on the *next* request that touches `/cart` (see [Purchase](#purchase-cart-based-checkout-server-side-persistence-no-auth)). The frontend doesn't special-case this at all — the checkout mutation simply invalidates the cached cart query on success, and the resulting background refetch naturally receives a fresh, empty, active cart from the server. The nav badge and Cart page both read from that same cached query, so they update to "0 items" together with no extra code.

## Deployment: Docker Compose

Phase 4, confirmed 2026-09-18. Three services — `postgres`, `backend`, `frontend` — defined in a root `docker-compose.yml`, satisfying the challenge's "runnable as a docker container" requirement via the multi-service reinterpretation already recorded under [Two `compose.yaml` files, on purpose](#two-composeyaml-files-on-purpose). Each of `backend/` and `frontend/` gets its own multi-stage `Dockerfile` (build stage compiles/bundles, runtime stage is a lean `eclipse-temurin:25-jre` / `nginx:alpine` image with only the built artifact).

**Only `frontend` publishes a host port.** The first draft mapped both `backend` (`8080:8080`) and `frontend` (`8080:80`) to the host, which collides — two containers can't bind the same host port. Since nginx already reverse-proxies API calls to `backend` over Docker Compose's internal network (`backend:8080`, resolved via Compose's built-in service-name DNS — no different from how `vite.config.ts`'s dev proxy targets `localhost:8080` in Phase 3), `backend` doesn't need to be reachable from the host at all. Final shape: `backend` has no `ports:` entry, `frontend` publishes `8080:80`, and a reviewer visits `http://localhost:8080` for everything — same-origin, same cookie behavior, same lack of any CORS configuration as the dev setup.

**`frontend/nginx.conf` mirrors the Vite dev proxy exactly** — the same four path prefixes (`/products`, `/cart`, `/orders`, `/import-jobs`) get proxied to `backend:8080`; everything else falls through to `try_files $uri /index.html`, so a hard refresh on a client-side route like `/orders/5` serves the SPA shell instead of an nginx 404, letting React Router take over.

**Postgres credentials are the same simple values already used in `backend/compose.yaml`** (`gilacommerce`/`gila_app`/`gilacommerce_dev_pw` — deliberately project-named rather than left as Spring Initializr's `mydatabase`/`myuser`/`secret` defaults, though still not a real secret worth protecting for a local challenge submission; a real production deployment would obviously not hardcode these at all).

**Postgres data is persisted to a named volume, in both compose files.** Confirmed 2026-09-18, added after losing local dev data to exactly the failure mode this prevents — without a volume, Postgres's data lives only in the container's writable layer and is gone the moment the container is removed (routine, e.g. after a credential change or an image update), not just on a crash. `backend/compose.yaml` gets `gila_dev_postgres_data`, the root `docker-compose.yml` gets its own separate `gila_postgres_data` — deliberately two different volumes, not shared, since dev data and "what a reviewer's stack produces" have no reason to mix.

**Mount point is `/var/lib/postgresql`, not the more commonly-documented `/var/lib/postgresql/data`.** The official Postgres image changed its expected layout at major version 18 (confirmed via this project's actual Postgres version, 18.6): it now organizes data into version-specific subdirectories under `/var/lib/postgresql` itself to support `pg_ctlcluster`-style major-version upgrades, and logs an explicit error refusing to start if a volume is mounted at the old `/data` path with existing data there. Caught by actually starting a container against the new config rather than trusting the (very common, but now outdated for v18+) online examples — verified for real with a create → write → remove → recreate → confirm-data-survived round trip before considering this done.

Verified end-to-end against locally-built images (tagged to match the GHCR names so Compose used the local cache instead of trying to pull) before the GHCR workflow had ever run: all three containers start, Flyway migrates against the containerized Postgres, and the full create/search/cart/checkout flow works through nginx's reverse proxy exactly as it did through Vite's dev proxy in Phase 3 — including the `HttpOnly` cart cookie surviving the proxy hop intact.

## CSV import

Example file provided by the challenge, saved at `docs/Code Challenge E-Commerce.csv`. **Download date: 2026-09-18.**

### Real-file validation
The Phase 1 import implementation was originally only exercised against a hand-written dummy CSV and the automated tests. The real file turned out to be deliberately adversarial — 97 data rows including a SQL injection attempt, an XSS payload, several malformed/blank rows, and repeated SKUs — so this became a genuine test of the design, not just a formatting check. Run against an isolated database (not the dev database, to avoid mixing test data): **90 of 97 rows imported successfully, 7 rejected individually**, each with a clear reason, no crash, no partial corruption:

| Row | Issue | Result |
|---|---|---|
| 4 | `price` = `$29.99` (currency symbol) | Rejected: "price must be a valid decimal number" |
| 7 | `price` = `free` | Rejected: "price must be a valid decimal number" |
| 16 | `stock` = `-5` | Rejected: "stock must not be negative" |
| 25 | `name` = empty | Rejected: "name is required" |
| 41 | `name` = whitespace-only | Rejected: "name is required" (`StringUtils.hasText` correctly treats whitespace as blank, not just `isEmpty()`) |
| 62, 63 | fully blank rows (`,,,,,,`) | Rejected: "sku is required" — **not** silently skipped by `setIgnoreEmptyLines(true)`, since a row of bare commas has content (empty fields), not zero characters; each still gets a reported reason |

None of these are code changes — malformed price/stock data being *rejected rather than coerced* (e.g. silently stripping the `$` or treating "free" as `0`) is the correct behavior for an import job whose whole design point is surfacing bad data individually rather than guessing at intent.

**The security-relevant rows were the actual point of this test, and both were handled correctly by construction, not by any CSV-specific defense:**
- **SQL injection** (`Robert'); DROP TABLE products;--` as a product name): stored as an inert literal string; verified all 9 tables still exist afterward. This works because the entire persistence layer goes through JPA/Hibernate (`productRepository.save(...)`), which always uses parameterized queries — there is no raw SQL string concatenation anywhere in this codebase for this to exploit. Nothing CSV-import-specific was needed; the same protection applies to every write path (the CRUD API included).
- **XSS** (`<script>alert('xss')</script>` as a product name): stored as an inert literal string, returned as-is in the JSON API (`"name":"<script>alert('xss')</script>"` — correct, JSON has no reason to HTML-escape this). Safety comes entirely from the frontend never bypassing React's default JSX escaping (confirmed: no `dangerouslySetInnerHTML` anywhere in `frontend/src`) — the payload renders as inert visible text wherever a product name appears, never as executed markup. Same principle as the SQL case: store raw, escape at the point of interpretation, never try to sanitize at write time (which would also corrupt legitimate data like a product literally named `<Product> & Co.`).

**Upsert-by-SKU was also exercised for real** by three SKUs repeated within the same file — `RS-001` (rows 2, 36) and `BS-021` (rows 11, 56, 89). Both ended up with exactly the last occurrence's values and a `version` incremented once per repeat (confirmed via direct DB query), and the 90 successful rows collapsed to 87 actual products — matching 90 minus the 3 "extra" occurrences of already-seen SKUs. Unicode (an em dash and a `™` in row 31's description) round-tripped correctly.

## Future improvements (time permitting)

Deliberately out of scope for the deadline, but noted here rather than silently skipped:

- **Async CSV import.** Return a `jobId` immediately and process the file on a background thread, with the frontend polling `GET /import-jobs/{id}` for status. Would matter for real supplier-sized files (tens of thousands of rows) where synchronous processing risks an HTTP timeout; not needed at this challenge's scale.
- **Testcontainers instead of H2 for integration tests.** Runs tests against a real Postgres container instead of an in-memory approximation, closing the gap on engine-specific behavior this project actually relies on (`@Version` optimistic locking semantics, SKU unique-constraint conflict behavior). Would be the preferred choice with more time; H2 was chosen for the deadline.
- **Product images.** Neither the challenge PDF nor the provided CSV columns (`name`, `sku`, `description`, `category`, `price`, `stock`, `weight_kg`) mention product images, so none of the schema/API/UI handles them. A real e-commerce catalog would need at least one image per product — noted here rather than silently absent, since it's a fairly conspicuous gap for the demo UI. Would need an image storage decision (object storage vs. a URL column) and isn't a small add, so deliberately left for later rather than squeezed into Phase 1/3.
- **Client-configurable sort on `GET /products`.** Currently hardcoded to `ORDER BY id` since the endpoint is a native `ILIKE` query (see [Search](#search-case-insensitive-partial-match-paginated)); supporting `?sort=` there would need a small allowlist mapping sort keys to safe column names to avoid building `ORDER BY` from unsanitized input.
- **"Thank you for your purchase" message + an Orders history section.** `/orders/:orderId` currently renders identically whether you just completed checkout or navigated there later (a saved link, a refresh). A one-time "Thank you!" banner shown only right after a successful checkout (e.g. passed via router navigation state, not shown on a cold visit to the same URL) plus a dedicated "Orders" page listing past orders would round out the purchase flow. Since this app has no user accounts (cart identity is an anonymous cookie only — see [Purchase](#purchase-cart-based-checkout-server-side-persistence-no-auth)), a real "my past orders" list needs a scope decision first: a lightweight browser-local history (an array of completed order IDs kept in `localStorage`) versus a bigger change (actual accounts). Noted here rather than assumed, since that decision shapes the whole feature.

## Running locally

### Full stack via Docker Compose

```
docker compose up
```
From the repo root. Pulls prebuilt images from GHCR and starts all three services — visit `http://localhost:8080`. See [Deployment: Docker Compose](#deployment-docker-compose) for how it's wired.

**One-time prerequisite**: the images must be published before this works — push to `main`, let `.github/workflows/publish-images.yml` run, then set both GHCR packages to public visibility in GitHub's package settings (see [Image hosting](#image-hosting-github-container-registry-ghcr--github-actions)). Without that, `docker compose up` will fail to pull.

### Individual services, for active development

Useful when iterating on either service rather than running the shipped images end-to-end.

**Backend** (from `backend/`):
```
./gradlew bootRun
```
Requires Docker Desktop running — `spring-boot-docker-compose` auto-starts a Postgres container from `backend/compose.yaml` on first run (see [Two `compose.yaml` files, on purpose](#two-composeyaml-files-on-purpose)). Serves the API on `http://localhost:8080`.

**Frontend** (from `frontend/`):
```
npm install
npm run dev
```
Serves the app on `http://localhost:5173`, proxying API calls through to the backend (see [Frontend](#frontend)) — no separate configuration needed as long as the backend is running on port 8080.

## Testing strategy

Confirmed 2026-09-16. Unit tests for service-layer logic, plus H2-backed Spring Boot integration tests for repository/import/checkout flows, rather than full end-to-end coverage — effort is concentrated on the parts with real logic/risk (CSV import upsert + per-row failure handling, checkout stock/version handling, search pagination) over CRUD boilerplate that's unlikely to be wrong. This is a scope tradeoff, not a claim of full coverage.

For checkout specifically: `CheckoutServiceIT` proves atomicity (the opposite property from CSV import's per-row isolation) by seeding a cart with one satisfiable line and one line exceeding stock, and asserting that *neither* product's stock changed after the rejected checkout — not just the one that failed. `CartControllerIT` proves the cookie lifecycle end-to-end (issued on first visit, reused across requests, replaced with a fresh one once the old cart is `CHECKED_OUT`). A genuine concurrent-checkout race (two requests racing the last unit of stock) is deliberately not automated — see the concurrency note under [Purchase](#purchase-cart-based-checkout-server-side-persistence-no-auth).

H2 was chosen over Testcontainers for the timeline — see [Future improvements](#future-improvements-time-permitting).

## AI usage disclosure

AI (Claude Code) was used during development to accelerate implementation and pressure-test design decisions. Per the challenge's explicit rule, AI-generated code comments are removed before submission.
