# Decisions & Assumptions

## 1. Assumptions Made Where Criteria Were Unclear
* **"Top rated" selection:** The acceptance criteria specify displaying 5 chosen "top rated" products on the Home page and top-rated products on Category pages. Without a curatorial admin tool, the backend deterministically ranks products by `rating.rate` descending (top 5 on Home; top 2 highlighted with a `★ Top Rated` badge on Category pages, followed by all remaining products in that category).
* **Search matching:** Search performs a case-insensitive substring match across both `title` and `description` via `GET /api/products/search?q=...`.
* **Enquiry form fields:** Captured Customer Name (required), Email Address (required, validated for `@`), and an optional Message, automatically linked to the selected product ID and title.

## 2. Requirements Dropped or Simplified (and Why)
* **Email Delivery Provider:** To satisfy Constraint #6 (no paid accounts or third-party credentials required for reviewers), `ConsoleEmailProvider` formats and logs the complete sales inbox email payload to standard output while returning HTTP `200 { "message": "Thank you" }`.
* **Authentication & Persistence:** Customer accounts, shopping cart state, and persistent database storage were excluded to stay focused on the critical path within the time-box.
* **Pagination:** Given the Fake Store catalogue size (20 items), endpoints return full filtered lists rather than cursor-based pagination.

## 3. How the Design Supports Growing Beyond the PoC
* **Strict Client/Server Boundary:** `CatalogueRepository` on Android communicates exclusively with `/api/products/*`, `/api/categories/*`, and `/api/enquiries`. The mobile client has zero coupling to Fake Store API schemas or endpoints.
* **Interface-Driven Backend Providers:** `ProductRepository` (`FakeStoreRepository`) and `EmailProvider` (`ConsoleEmailProvider`) are abstracted behind TypeScript interfaces. Swapping in a custom PostgreSQL/DynamoDB catalogue or **AWS SES** email delivery requires adding a new class implementation without modifying Express routes or the Android client.
* **AWS Compatibility:** The Node.js/TypeScript backend is stateless and environment-configured (`PORT`, `STORE_API_BASE_URL`, `SALES_INBOX_EMAIL`), making it ready to containerize and deploy to **AWS ECS / Fargate** or **AWS App Runner** behind an Application Load Balancer.

## 4. What I Would Do Next With Another Day
1. Add `Dockerfile` and `docker-compose.yml` for single-command containerized startup and AWS ECS deployment.
2. Implement `SesEmailProvider` backed by AWS SES and persist enquiries in Amazon DynamoDB or PostgreSQL.
3. Add backend Jest/Supertest integration tests alongside Compose UI tests (`createComposeRule`) and offline caching on Android.