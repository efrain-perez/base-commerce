# CLAUDE.md — E-Commerce Code Challenge

## Context

Code challenge for a Java position interview process. Deadline: **Wednesday, Sep 23**. Started: Sep 15.
Full requirements are in the original challenge PDF (not included here) — summary: enterprise-grade e-commerce app, CRUD for Products, CSV import, search, purchase (fake payment), UI required, must run via Docker, README with decisions/alternatives, GitHub repo.

**Critical note from the challenge itself:** the evaluators explicitly say this isn't about completing the CRUD (trivial with AI) — they're grading the questions asked and the design judgment shown. Every non-obvious decision should be justified in the README.

**AI usage rule from the challenge:** AI is allowed, but any AI-generated comments must be removed from the final code before submission.

## Tech Stack (decided)

- Backend: Spring Boot, layered architecture (Controller → Service → Repository), DTOs separate from entities
- DB: PostgreSQL + Flyway for versioned migrations
- Frontend: React SPA (separate from backend, not server-rendered) — deliberately chosen over Thymeleaf despite more work, to reinforce React practice for full-stack roles
- Docker: single `docker-compose.yml` with 3 services (postgres, backend, frontend/nginx) — challenge says "container" singular; compose is the reasonable enterprise-grade interpretation, note this explicitly in the README

## Key design decisions

**Product versioning (`@Version` on `Product`, JPA optimistic locking):**
One field serves two purposes — optimistic locking solves concurrent-purchase race conditions (two users buying the last unit of stock), and the same version number lets the app detect and surface price changes on items sitting in a cart. `PurchaseItem`/`OrderItem` stores a snapshot (`product_id`, `name`, `price_at_purchase`, `version_at_purchase`) rather than a full product-version-history table — enough to answer "user X bought Y at version Z for price P" without extra scope.

**CSV import as an auditable event, not just a data load:**
`ImportJob` entity (`file_name`, `executed_at`, `total_rows`, `success_count`, `failure_count`, `status`) + `ImportJobError` entity (`import_job_id`, `row_number`, `raw_data`, `error_reason`). A bad row fails and is logged individually; the rest of the import continues. No UI needed for this — expose read-only via API if time allows, otherwise just document it as a design decision.

**Real CSV file:** the challenge provides an example CSV and asks the README to state the download date. It will be downloaded **Saturday Sep 19** (not yet downloaded as of this file's creation) — do not fabricate an earlier date. Import logic should be built/tested against a self-made dummy CSV in the meantime, then validated against the real one once downloaded.

## Decisions confirmed 2026-09-16

- **Duplicate SKU on import: upsert by SKU.** Re-importing a CSV with an existing SKU updates that product's fields (name, price, stock, etc.), bumping `@Version` — which naturally feeds the existing price-change-detection design rather than needing separate logic.
- **Invalid row on import:** fail only that row, continue the rest, report failures (see ImportJob/ImportJobError above)
- **Search matching: case-insensitive, partial match** (SQL `ILIKE %term%` or trigram index). Full-text search with ranking was considered and deferred as out of scope — note this tradeoff in the README.
- **Search/listing pagination: yes**, standard page/size query params (Spring Data `Pageable`).

## Working style for this project

- Every non-trivial decision should be logged with rationale — Efrain needs to fully understand and be able to explain each part to clients/interviewers, not just have working code
- When Claude proposes a design, flag it clearly as a proposal so Efrain can confirm before it's treated as final
- Remove any AI-flavored comments from generated code before finalizing, per the challenge's explicit instruction
